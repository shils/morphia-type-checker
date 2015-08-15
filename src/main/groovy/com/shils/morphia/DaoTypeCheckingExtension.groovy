package com.shils.morphia


import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import groovyjarjarasm.asm.Opcodes
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
import org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport
import org.mongodb.morphia.annotations.Property
import org.mongodb.morphia.annotations.Transient
import org.mongodb.morphia.query.Query
import org.mongodb.morphia.query.UpdateOperations

@InheritConstructors
@CompileStatic
class DaoTypeCheckingExtension extends AbstractTypeCheckingExtension implements Opcodes {

  static final ClassNode UPDATE_OPERATIONS_TYPE = ClassHelper.make(UpdateOperations.class)
  static final ClassNode QUERY_TYPE = ClassHelper.make(Query.class)
  static final ClassNode TRANSIENT_TYPE = ClassHelper.make(Transient.class)
  static final ClassNode PROPERTY_TYPE = ClassHelper.make(Property.class)

  ClassNode entityType
  @Override
  void afterMethodCall(MethodCall call) {
    ASTNode receiver = call.receiver
    if (receiver instanceof Expression) {
      if (StaticTypeCheckingSupport.implementsInterfaceOrIsSubclassOf(((Expression) receiver).type, QUERY_TYPE)) {
        validateQueryMethodCall(call)
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
        validateFieldArgument(argValue, fieldArgExpr)
        break
      case 'filter':
        validateFieldArgument(argValue.split(' ').first(), fieldArgExpr)
        break
      case 'order':
        validateFieldArgument(argValue.startsWith('-') ? argValue.substring(1) : argValue, fieldArgExpr)
        break
    }
  }

  private void validateFieldArgument(String fieldArgument, ASTNode argumentNode) {
    String[] fieldNames = fieldArgument.split('\\.')
    ClassNode ownerType = entityType
    for (String fieldName: fieldNames) {
      FieldNode field = ownerType.getField(fieldName) ?: findFieldByPropertyName(ownerType, fieldName)
      if (!field || field.isStatic() || isFieldTransient(field)) {
        addNoPersistedFieldError(fieldName, ownerType, argumentNode)
        return
      }
      ownerType = field.type
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