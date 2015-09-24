package me.shils.morphia


import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import me.shils.morphia.annotations.FieldName
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCall
import org.mongodb.morphia.annotations.Entity
import org.mongodb.morphia.query.Query
import org.mongodb.morphia.query.UpdateOperations

import static groovy.transform.Undefined.isUndefined
import static org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport.implementsInterfaceOrIsSubclassOf

/**
 * A type checking extension for classes extending {@link org.mongodb.morphia.dao.DAO}.
 *
 * @author Shil Sinha
 */
@InheritConstructors
@CompileStatic
class DAOTypeCheckingExtension extends MorphiaTypeCheckingExtension {

  private static final ClassNode UPDATE_OPERATIONS_TYPE = ClassHelper.make(UpdateOperations.class)
  private static final ClassNode QUERY_TYPE = ClassHelper.make(Query.class)
  private static final ClassNode FIELD_NAME_TYPE = ClassHelper.make(FieldName.class)
  private static final ClassNode ENTITY_TYPE = ClassHelper.make(Entity.class)

  @Override
  ClassNode currentEntityType(){
    return getEnclosingClassNode().getUnresolvedSuperClass(false).genericsTypes[0].type
  }

  @Override
  void afterMethodCall(MethodCall call) {
    ASTNode receiver = call.receiver
    if (!receiver || call.methodAsString == 'enableValidation')
      return

    if (receiver.getNodeMetaData(ValidationMarker.DISABLED) || call.methodAsString == 'disableValidation') {
      callAsExpression(call).putNodeMetaData(ValidationMarker.DISABLED, true)
      return
    }

    if (receiver instanceof VariableExpression && receiver.isThisExpression()) {
      validateDAOMethodCall(call)
      return
    }
    ClassNode receiverType = receiver instanceof ClassNode ? (ClassNode) receiver : getType(receiver)
    if (implementsInterfaceOrIsSubclassOf(receiverType, QUERY_TYPE)) {
      validateQueryMethodCall(call)
    } else if (implementsInterfaceOrIsSubclassOf(receiverType, UPDATE_OPERATIONS_TYPE)) {
      validateUpdateOpsMethodCall(call)
    } else {
      validateFieldNameUsage(call)
    }
  }

  private void validateQueryMethodCall(MethodCall call) {
    Expression fieldArgExpr = ((ArgumentListExpression) call.arguments).expressions.find()
    if (!fieldArgExpr || !(fieldArgExpr instanceof ConstantExpression) || !(((ConstantExpression) fieldArgExpr).value instanceof String))
      return

    String argValue = (String) ((ConstantExpression) fieldArgExpr).value
    switch (call.methodAsString) {
      case 'field':
      case 'criteria':
        resolveFieldArgument(argValue, fieldArgExpr)
        break
      case 'filter':
        resolveFieldArgument(argValue.split(' ').first(), fieldArgExpr)
        break
      case 'order':
        resolveFieldArgument(argValue.startsWith('-') ? argValue.substring(1) : argValue, fieldArgExpr)
        break
      default:
        validateFieldNameUsage(call)
    }
  }

  private void validateUpdateOpsMethodCall(MethodCall call) {
    Expression fieldArgExpr = ((ArgumentListExpression) call.arguments).expressions.find()
    if (!fieldArgExpr || !(fieldArgExpr instanceof ConstantExpression) || !(((ConstantExpression) fieldArgExpr).value instanceof String))
      return

    String argValue = (String) ((ConstantExpression) fieldArgExpr).value
    switch (call.methodAsString) {
      case 'set':
      case 'setOnInsert':
      case 'unset':
        resolveFieldArgument(argValue, fieldArgExpr)
        break
      case 'add':
      case 'addAll':
      case 'removeFirst':
      case 'removeLast':
      case 'removeAll':
        validateArrayFieldArgument(argValue, fieldArgExpr)
        break
      case 'dec':
      case 'inc':
      case 'max':
      case 'min':
        validateNumericFieldArgument(argValue, fieldArgExpr)
        break
      default:
        validateFieldNameUsage(call)
    }
  }

  private void validateDAOMethodCall(MethodCall call) {
    //expect that DAO methods with String 'field' parameters will also have Object 'value' parameters
    if (((ArgumentListExpression) call.arguments).expressions.size() < 2)
      return

    Expression fieldArgExpr = ((ArgumentListExpression) call.arguments).expressions.first()
    if (!(fieldArgExpr instanceof ConstantExpression) || !(((ConstantExpression) fieldArgExpr).value instanceof String))
      return

    String argValue = (String) ((ConstantExpression) fieldArgExpr).value
    switch (call.methodAsString) {
      case 'findIds':
      case 'findOneId':
      case 'exists':
      case 'count':
      case 'findOne':
        resolveFieldArgument(argValue, fieldArgExpr)
        break
      default:
        validateFieldNameUsage(call)
    }
  }

  private void validateFieldNameUsage(MethodCall methodCall) {
    MethodNode method = getTargetMethod(callAsExpression(methodCall)) ?: methodCall instanceof MethodCallExpression ?
            ((MethodCallExpression) methodCall).methodTarget : null
    if (!method || !method.parameters)
      return

    ArgumentListExpression args = getArguments((MethodCall) methodCall)

    method.parameters.eachWithIndex { Parameter it, int index ->
      AnnotationNode fieldNameUsage = it.getAnnotations(FIELD_NAME_TYPE).find()
      if (!fieldNameUsage)
        return

      ClassNode fieldType = resolveFieldNameTarget(fieldNameUsage, method, args)
      if (!fieldType || !isAnnotatedBy(fieldType.redirect(), ENTITY_TYPE))
        return

      Expression fieldArgExpression = args.getExpression(index)
      if (!(fieldArgExpression instanceof ConstantExpression) || fieldArgExpression.isNullExpression())
        return

      resolveFieldArgument(fieldType, ((ConstantExpression) fieldArgExpression).text, fieldArgExpression)
    }
  }

  ClassNode resolveFieldNameTarget(AnnotationNode fieldNameUsage, MethodNode method, ArgumentListExpression args) {
    Expression value = fieldNameUsage.getMember('value')
    if (value instanceof ClassExpression && !isUndefined(value.type)) {
      return value.type
    }

    Expression parameterIndexMember = fieldNameUsage.getMember('parameterIndex')
    Integer parameterIndex = null
    if (parameterIndexMember) {
      parameterIndex = (int) ((ConstantExpression) parameterIndexMember).value
    }

    if (parameterIndex == null || parameterIndex < 0 || parameterIndex >= method.parameters.length) {
      addStaticTypeError('error', fieldNameUsage)
      return null
    }

    ClassNode targetType = getType(args.getExpression(parameterIndex))

    if (!targetType.isUsingGenerics())
      return targetType

    Expression genericTypeIndexMember = fieldNameUsage.getMember('genericTypeIndex')
    if (!genericTypeIndexMember)
      return targetType

    int genericTypeIndex = (int) ((ConstantExpression) genericTypeIndexMember).value
    if (genericTypeIndex < 0 || genericTypeIndex >= targetType.genericsTypes.length ) {
      addStaticTypeError('error', fieldNameUsage)
      return null
    }

    return targetType.genericsTypes[genericTypeIndex].type
  }

  private static Expression callAsExpression(MethodCall call) {
    if (call instanceof ConstructorCallExpression)
      return (ConstructorCallExpression) call
    else if (call instanceof MethodCallExpression)
      return (MethodCallExpression) call
    else if (call instanceof StaticMethodCallExpression)
      return (StaticMethodCallExpression) call

    return null
  }

  private enum ValidationMarker {
    DISABLED
  }
}