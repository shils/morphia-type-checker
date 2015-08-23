package com.shils.morphia

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.codehaus.groovy.ast.GenericsType
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Serialized
import org.objectweb.asm.Opcodes
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.transform.stc.AbstractTypeCheckingExtension
import org.mongodb.morphia.annotations.Property
import org.mongodb.morphia.annotations.Transient

import static org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport.implementsInterfaceOrIsSubclassOf

/**
 *
 * @author Shil Sinha
 */
@InheritConstructors
@CompileStatic
abstract class MorphiaTypeCheckingExtension extends AbstractTypeCheckingExtension implements Opcodes {

  static final ClassNode TRANSIENT_TYPE = ClassHelper.make(Transient.class)
  static final ClassNode EMBEDDED_TYPE = ClassHelper.make(Embedded.class)
  static final ClassNode PROPERTY_TYPE = ClassHelper.make(Property.class)
  static final ClassNode REFERENCE_TYPE = ClassHelper.make(org.mongodb.morphia.annotations.Reference)
  static final ClassNode SERIALIZED_TYPE = ClassHelper.make(Serialized.class)
  static final ClassNode COLLECTION_TYPE = ClassHelper.make(Collection.class)

  static final ClassNode[] NAME_OVERRIDING_TYPES = [EMBEDDED_TYPE, PROPERTY_TYPE, REFERENCE_TYPE, SERIALIZED_TYPE] as ClassNode[]

  abstract ClassNode currentEntityType()

  FieldNode resolveFieldArgument(String fieldArgument, ASTNode argumentNode) {
    String[] fieldNames = fieldArgument.split('\\.')
    ClassNode ownerType = currentEntityType()
    FieldNode field = null
    for (String fieldName: fieldNames) {
      if (field && implementsInterfaceOrIsSubclassOf(field.type, ClassHelper.MAP_TYPE)) {
        field = null
        continue
      }

      field = ownerType.getField(fieldName) ?: findFieldByOverridingName(ownerType, fieldName)
      if (!field || field.isStatic() || isFieldTransient(field)) {
        addNoPersistedFieldError(fieldName, ownerType, argumentNode)
        return null
      } else if (field.type.isArray()) {
        ownerType = field.type.componentType
      } else if (implementsInterfaceOrIsSubclassOf(field.type, COLLECTION_TYPE)) {
        ownerType = field.type.usingGenerics ? extractGenericUpperBoundOrType(field.type, 0) : ClassHelper.OBJECT_TYPE
      } else if (implementsInterfaceOrIsSubclassOf(field.type, ClassHelper.MAP_TYPE)) {
        ownerType = field.type.usingGenerics ? extractGenericUpperBoundOrType(field.type, 1) : ClassHelper.OBJECT_TYPE
      } else {
        ownerType = field.type
      }
    }
    return field
  }

  void validateArrayFieldArgument(String fieldArgument, ASTNode argumentNode) {
    FieldNode field = resolveFieldArgument(fieldArgument, argumentNode)
    if (field && !implementsInterfaceOrIsSubclassOf(field.type, COLLECTION_TYPE) && !field.type.isArray()){
      addStaticTypeError("$fieldArgument does not refer to an array field", argumentNode)
    }
  }

  void validateNumericFieldArgument(String fieldArgument, ASTNode argumentNode) {
    FieldNode field = resolveFieldArgument(fieldArgument, argumentNode)
    if (field && !implementsInterfaceOrIsSubclassOf(ClassHelper.getWrapper(field.type), ClassHelper.Number_TYPE)){
      addStaticTypeError("$fieldArgument does not refer to a numeric field", argumentNode)
    }
  }

  void addNoPersistedFieldError(String fieldName, ClassNode ownerType, ASTNode errorNode) {
    addStaticTypeError("No such persisted field: $fieldName for class: ${ownerType.getName()}".toString(), errorNode)
  }

  static FieldNode findFieldByOverridingName(ClassNode ownerType, String overridingName) {
    return ownerType.fields.find { field ->
      AnnotationNode anno = field.getAnnotations().find {
        NAME_OVERRIDING_TYPES.contains(it.classNode)
      }
      Expression expr = anno?.getMember('value')
      (expr instanceof ConstantExpression) && ((ConstantExpression) expr).value == overridingName
    }
  }

  static boolean isFieldTransient(FieldNode field) {
    return field.modifiers & ACC_TRANSIENT || field.getAnnotations(TRANSIENT_TYPE)
  }

  static ClassNode extractGenericUpperBoundOrType(ClassNode node, int genericsTypeIndex) {
    GenericsType genericsType = node.genericsTypes[genericsTypeIndex]
    return (ClassNode) genericsType.upperBounds.find() ?: genericsType.type
  }
}