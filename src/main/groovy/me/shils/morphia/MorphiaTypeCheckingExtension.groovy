package me.shils.morphia

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import groovy.transform.PackageScope
import org.bson.types.ObjectId
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

  private static final ClassNode TRANSIENT_TYPE = ClassHelper.make(Transient.class)
  private static final ClassNode EMBEDDED_TYPE = ClassHelper.make(Embedded.class)
  private static final ClassNode PROPERTY_TYPE = ClassHelper.make(Property.class)
  private static final ClassNode REFERENCE_TYPE = ClassHelper.make(org.mongodb.morphia.annotations.Reference)
  private static final ClassNode SERIALIZED_TYPE = ClassHelper.make(Serialized.class)
  private static final ClassNode COLLECTION_TYPE = ClassHelper.make(Collection.class)
  private static final ClassNode OBJECT_ID_TYPE = ClassHelper.make(ObjectId.class)

  private static final ClassNode[] NAME_OVERRIDING_TYPES = [EMBEDDED_TYPE, PROPERTY_TYPE, REFERENCE_TYPE, SERIALIZED_TYPE] as ClassNode[]
  private static final ClassNode[] CANNOT_QUERY_PAST_TYPES = [REFERENCE_TYPE, SERIALIZED_TYPE] as ClassNode[]
  private static final String MONGO_ID_FIELD_NAME = '_id'

  abstract ClassNode currentEntityType()

  protected ClassNode resolveFieldArgument(String fieldArgument, ASTNode argumentNode) {
    resolveFieldArgument(currentEntityType(), fieldArgument, argumentNode)
  }

  /**
   * Resolves a chain of field accesses and returns the type of the result
   * @param ownerType
   * @param fieldArgument the field access chain String
   * @param argumentNode the ASTNode representing the field access chain String
   * @return the type of the result of the chain of field accesses, or null if there is a validation error
   */
  protected ClassNode resolveFieldArgument(ClassNode ownerType, String fieldArgument, ASTNode argumentNode) {
    String[] fieldNames = fieldArgument.split('\\.')
    int index = 0
    if (MONGO_ID_FIELD_NAME == fieldNames[0] && !ownerType.getField(MONGO_ID_FIELD_NAME)) {
      ownerType = OBJECT_ID_TYPE
      index++
    }

    FieldNode field = null
    ClassNode previousOwnerType = null
    while (index < fieldNames.length) {
      AnnotationNode cannotQueryPastAnnotation = field?.getAnnotations()?.find { CANNOT_QUERY_PAST_TYPES.contains(it.classNode) }
      if (cannotQueryPastAnnotation) {
        addStaticTypeError("Cannot access fields of ${previousOwnerType.name}.$field.name since it is annotated with @${cannotQueryPastAnnotation.classNode.name}".toString(), argumentNode)
        return null
      }

      String fieldName = fieldNames[index++]
      field = ownerType.getField(fieldName) ?: findFieldByOverridingName(ownerType, fieldName)

      if (!field || field.isStatic() || isFieldTransient(field)) {
        addNoPersistedFieldError(fieldName, ownerType, argumentNode)
        return null
      }

      previousOwnerType = ownerType
      if (field.type.isArray()) {
        ownerType = field.type.componentType
      } else if (implementsInterfaceOrIsSubclassOf(field.type, COLLECTION_TYPE)) {
        ownerType = field.type.usingGenerics ? extractGenericUpperBoundOrType(field.type, 0) : ClassHelper.OBJECT_TYPE
      } else if (implementsInterfaceOrIsSubclassOf(field.type, ClassHelper.MAP_TYPE)) {
        ownerType = field.type.usingGenerics ? extractGenericUpperBoundOrType(field.type, 1) : ClassHelper.OBJECT_TYPE
        index++
      } else {
        ownerType = field.type
      }
    }
    //hack for '_id' field arguments, where the variable 'field' doesn't refer to a real field, but its type is known
    return field?.type ?: ownerType
  }

  protected void validateArrayFieldArgument(String fieldArgument, ASTNode argumentNode) {
    ClassNode fieldType = resolveFieldArgument(fieldArgument, argumentNode)
    if (fieldType && !implementsInterfaceOrIsSubclassOf(fieldType, COLLECTION_TYPE) && !fieldType.isArray()){
      addStaticTypeError("$fieldArgument does not refer to an array field", argumentNode)
    }
  }

  protected void validateNumericFieldArgument(String fieldArgument, ASTNode argumentNode) {
    ClassNode fieldType = resolveFieldArgument(fieldArgument, argumentNode)
    if (fieldType && !implementsInterfaceOrIsSubclassOf(ClassHelper.getWrapper(fieldType), ClassHelper.Number_TYPE)){
      addStaticTypeError("$fieldArgument does not refer to a numeric field", argumentNode)
    }
  }

  protected void addNoPersistedFieldError(String fieldName, ClassNode ownerType, ASTNode errorNode) {
    addStaticTypeError("No such persisted field: $fieldName for class: ${ownerType.getName()}".toString(), errorNode)
  }

  @PackageScope
  static FieldNode findFieldByOverridingName(ClassNode ownerType, String overridingName) {
    return ownerType.fields.find { field ->
      AnnotationNode anno = field.getAnnotations().find {
        NAME_OVERRIDING_TYPES.contains(it.classNode)
      }
      Expression expr = anno?.getMember('value')
      (expr instanceof ConstantExpression) && ((ConstantExpression) expr).value == overridingName
    }
  }

  @PackageScope
  static boolean isFieldTransient(FieldNode field) {
    return field.modifiers & ACC_TRANSIENT || field.getAnnotations(TRANSIENT_TYPE)
  }

  @PackageScope
  static ClassNode extractGenericUpperBoundOrType(ClassNode node, int genericsTypeIndex) {
    GenericsType genericsType = node.genericsTypes[genericsTypeIndex]
    return (ClassNode) genericsType.upperBounds.find() ?: genericsType.type
  }
}