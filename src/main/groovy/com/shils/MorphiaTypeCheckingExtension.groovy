package com.shils


import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCall
import org.codehaus.groovy.transform.stc.AbstractTypeCheckingExtension
import org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport
import org.mongodb.morphia.annotations.Transient
import org.mongodb.morphia.query.Query
import org.mongodb.morphia.query.UpdateOperations

@InheritConstructors
@CompileStatic
class MorphiaTypeCheckingExtension extends AbstractTypeCheckingExtension implements Opcodes {

  ClassNode entityType
  static final ClassNode UPDATE_OPERATIONS_TYPE = ClassHelper.make(UpdateOperations.class)
  static final ClassNode QUERY_TYPE = ClassHelper.make(Query.class)
  static final ClassNode TRANSIENT_TYPE = ClassHelper.make(Transient.class)

  @Override
  void afterMethodCall(MethodCall call) {
    ASTNode receiver = call.receiver
    if (receiver instanceof Expression) {
      if (StaticTypeCheckingSupport.implementsInterfaceOrIsSubclassOf(((Expression) receiver).type, QUERY_TYPE) && call.methodAsString == 'field') {
        validateQueryFieldCall(call)
      }
    }
  }

  boolean beforeVisitClass(ClassNode classNode) {
    setEntityType(classNode.getUnresolvedSuperClass(false).genericsTypes[0].type)
    false
  }

  void validateQueryFieldCall(MethodCall call) {
    String fieldName = ((ConstantExpression)((ArgumentListExpression) call.arguments).expressions.first()).text
    FieldNode entityField = entityType.getField(fieldName)
    if (!entityField || entityField.isStatic() || isFieldTransient(entityField)) {
      addStaticTypeError("No such field: $fieldName for class: ${entityType.getNameWithoutPackage()}".toString(), call.receiver)
    }
  }

  private static boolean isFieldTransient(FieldNode field) {
    return field.modifiers & ACC_TRANSIENT || field.getAnnotations(TRANSIENT_TYPE)
  }

}