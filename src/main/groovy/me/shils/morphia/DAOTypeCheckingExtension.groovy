package me.shils.morphia


import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
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
import org.mongodb.morphia.query.Query
import org.mongodb.morphia.query.UpdateOperations

import static org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport.implementsInterfaceOrIsSubclassOf

/**
 * A type checking extension for classes extending {@link org.mongodb.morphia.dao.DAO}.
 *
 * @author Shil Sinha
 */
@InheritConstructors
@CompileStatic
class DAOTypeCheckingExtension extends MorphiaTypeCheckingExtension {

  static final ClassNode UPDATE_OPERATIONS_TYPE = ClassHelper.make(UpdateOperations.class)
  static final ClassNode QUERY_TYPE = ClassHelper.make(Query.class)

  @Override
  ClassNode currentEntityType(){
    return getEnclosingClassNode().getUnresolvedSuperClass(false).genericsTypes[0].type
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

  private void validateQueryMethodCall(MethodCall call) {
    Expression fieldArgExpr = ((ArgumentListExpression) call.arguments).expressions.find()
    if (!fieldArgExpr || !(fieldArgExpr instanceof ConstantExpression) || !(((ConstantExpression) fieldArgExpr).value instanceof String))
      return

    String argValue = (String) ((ConstantExpression) fieldArgExpr).value
    switch (call.methodAsString) {
      case 'field':
        resolveFieldQuery(argValue, fieldArgExpr)
        break
      case 'filter':
        resolveFieldQuery(argValue.split(' ').first(), fieldArgExpr)
        break
      case 'order':
        resolveFieldQuery(argValue.startsWith('-') ? argValue.substring(1) : argValue, fieldArgExpr)
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
        resolveFieldQuery(argValue, fieldArgExpr)
        break
      case 'add':
      case 'addAll':
      case 'removeFirst':
      case 'removeLast':
      case 'removeAll':
        validateArrayFieldQuery(argValue, fieldArgExpr)
        break
      case 'dec':
      case 'inc':
      case 'max':
      case 'min':
        validateNumericFieldQuery(argValue, fieldArgExpr)
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
        resolveFieldQuery(argValue, fieldArgExpr)
    }
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

  enum ValidationMarker {
    DISABLED
  }
}