package me.shils.morphia


import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.codehaus.groovy.ast.MethodNode
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
import org.mongodb.morphia.dao.DAO
import org.mongodb.morphia.query.Query
import org.mongodb.morphia.query.UpdateOperations

import java.lang.reflect.Modifier

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
  private static final ClassNode DAO_TYPE = ClassHelper.make(DAO.class)

  @Override
  ClassNode currentEntityType(){
    findEnclosingEntityType()
  }

  @Override
  void afterMethodCall(MethodCall call) {
    ASTNode receiver = call.receiver
    if (!receiver || !(receiver instanceof Expression) || call.methodAsString == 'enableValidation')
      return

    if (receiver.getNodeMetaData(ValidationMarker.DISABLED) || call.methodAsString == 'disableValidation') {
      callAsExpression(call).putNodeMetaData(ValidationMarker.DISABLED, true)
      return
    }

    if (receiver instanceof VariableExpression && receiver.isThisExpression()) {
      validateDAOMethodCall(call)
      return
    }
    ClassNode receiverType = getType(receiver)
    if (implementsInterfaceOrIsSubclassOf(receiverType, QUERY_TYPE)) {
      validateQueryMethodCall(call)
    } else if (implementsInterfaceOrIsSubclassOf(receiverType, UPDATE_OPERATIONS_TYPE)) {
      validateUpdateOpsMethodCall(call)
    }
  }

  @Override
  boolean beforeVisitMethod(MethodNode node) {
    return !findEnclosingEntityType()
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
        validateFieldArguments((ConstantExpression) fieldArgExpr)
        break
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
    }
  }

  private ClassNode findEnclosingEntityType() {
    ClassNode cn = getEnclosingClassNode()
    while (cn && !cn.implementsInterface(DAO_TYPE)) {
      if (Modifier.isStatic(cn.modifiers)) {
        cn = null
        break
      }
      cn = cn.outerClass
    }
    cn ? extractEntityType(cn) : null
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

  private static ClassNode extractEntityType(ClassNode node) {
    node.getUnresolvedSuperClass(false).genericsTypes[0].type
  }

  private enum ValidationMarker {
    DISABLED
  }
}