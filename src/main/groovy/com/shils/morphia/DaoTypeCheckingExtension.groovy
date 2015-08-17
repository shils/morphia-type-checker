package com.shils.morphia


import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.objectweb.asm.Opcodes
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCall
import org.codehaus.groovy.transform.stc.AbstractTypeCheckingExtension
import org.mongodb.morphia.annotations.Property
import org.mongodb.morphia.annotations.Transient
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
class DaoTypeCheckingExtension extends AbstractTypeCheckingExtension implements Opcodes {

  static final ClassNode UPDATE_OPERATIONS_TYPE = ClassHelper.make(UpdateOperations.class)
  static final ClassNode QUERY_TYPE = ClassHelper.make(Query.class)
  static final ClassNode TRANSIENT_TYPE = ClassHelper.make(Transient.class)
  static final ClassNode PROPERTY_TYPE = ClassHelper.make(Property.class)
  static final ClassNode COLLECTION_TYPE = ClassHelper.make(Collection.class)

  ClassNode entityType
  @Override
  void afterMethodCall(MethodCall call) {
    ASTNode receiver = call.receiver
    if (receiver instanceof Expression) {
      ClassNode receiverType = ((Expression) receiver).type
      if (implementsInterfaceOrIsSubclassOf(receiverType, QUERY_TYPE)) {
        validateQueryMethodCall(call)
      } else if (implementsInterfaceOrIsSubclassOf(receiverType, UPDATE_OPERATIONS_TYPE)) {
        validateUpdateOpsMethodCall(call)
      }
    }
  }

  boolean beforeVisitClass(ClassNode classNode) {
    setEntityType(classNode.getUnresolvedSuperClass(false).genericsTypes[0].type)
    return false
  }

  private void validateQueryMethodCall(MethodCall call) {
    Expression fieldArgExpr = ((ArgumentListExpression) call.arguments).expressions.first()
    if (!(fieldArgExpr instanceof ConstantExpression) || !(((ConstantExpression) fieldArgExpr).value instanceof String))
      return

    String argValue = (String) ((ConstantExpression) fieldArgExpr).value
    switch (call.methodAsString) {
      case 'field':
        resolveFieldArgument(argValue, fieldArgExpr)
        break
      case 'filter':
        resolveFieldArgument(argValue.split(' ').first(), fieldArgExpr)
        break
      case 'order':
        resolveFieldArgument(argValue.startsWith('-') ? argValue.substring(1) : argValue, fieldArgExpr)
        break
    }
  }

  private void validateUpdateOpsMethodCall(MethodCall call) {
    Expression fieldArgExpr = ((ArgumentListExpression) call.arguments).expressions.first()
    if (!(fieldArgExpr instanceof ConstantExpression) || !(((ConstantExpression) fieldArgExpr).value instanceof String))
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

  private FieldNode resolveFieldArgument(String fieldArgument, ASTNode argumentNode) {
    String[] fieldNames = fieldArgument.split('\\.')
    ClassNode ownerType = entityType
    FieldNode field = null
    for (String fieldName: fieldNames) {
      field = ownerType.getField(fieldName) ?: findFieldByPropertyName(ownerType, fieldName)
      if (!field || field.isStatic() || isFieldTransient(field)) {
        addNoPersistedFieldError(fieldName, ownerType, argumentNode)
        return null
      }
      ownerType = field.type
    }
    return field
  }

  private void validateArrayFieldArgument(String fieldArgument, ASTNode argumentNode) {
    FieldNode field = resolveFieldArgument(fieldArgument, argumentNode)
    if (field && !implementsInterfaceOrIsSubclassOf(field.type, COLLECTION_TYPE) && !field.type.isArray()){
      addStaticTypeError("$fieldArgument does not refer to an array field", argumentNode)
    }
  }

  private void validateNumericFieldArgument(String fieldArgument, ASTNode argumentNode) {
    FieldNode field = resolveFieldArgument(fieldArgument, argumentNode)
    if (field && !implementsInterfaceOrIsSubclassOf(ClassHelper.getWrapper(field.type), ClassHelper.Number_TYPE)){
      addStaticTypeError("$fieldArgument does not refer to a numeric field", argumentNode)
    }
  }

  private static FieldNode findFieldByPropertyName(ClassNode ownerType, String propertyName) {
    return ownerType.fields.find {
      AnnotationNode anno = it.getAnnotations(PROPERTY_TYPE).find()
      Expression expr = anno?.getMember('value')
      (expr instanceof ConstantExpression) && ((ConstantExpression) expr).value == propertyName
    }
  }

  private void addNoPersistedFieldError(String fieldName, ClassNode ownerType, ASTNode errorNode) {
    addStaticTypeError("No such persisted field: $fieldName for class: ${ownerType.getName()}".toString(), errorNode)
  }

  private static boolean isFieldTransient(FieldNode field) {
    return field.modifiers & ACC_TRANSIENT || field.getAnnotations(TRANSIENT_TYPE)
  }

}