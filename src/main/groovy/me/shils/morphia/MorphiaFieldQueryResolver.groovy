package me.shils.morphia

import groovy.transform.CompileStatic
import org.bson.types.ObjectId
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.GenericsType
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.transform.stc.TypeCheckingExtension
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Property
import org.mongodb.morphia.annotations.Serialized
import org.mongodb.morphia.annotations.Transient
import org.objectweb.asm.Opcodes

import static org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport.implementsInterfaceOrIsSubclassOf

/**
 *
 * @author Shil Sinha
 */
@CompileStatic
class MorphiaFieldQueryResolver implements Opcodes {

  static final ClassNode TRANSIENT_TYPE = ClassHelper.make(Transient.class)
  static final ClassNode EMBEDDED_TYPE = ClassHelper.make(Embedded.class)
  static final ClassNode PROPERTY_TYPE = ClassHelper.make(Property.class)
  static final ClassNode REFERENCE_TYPE = ClassHelper.make(org.mongodb.morphia.annotations.Reference)
  static final ClassNode SERIALIZED_TYPE = ClassHelper.make(Serialized.class)
  static final ClassNode COLLECTION_TYPE = ClassHelper.make(Collection.class)
  static final ClassNode OBJECT_ID_TYPE = ClassHelper.make(ObjectId.class)

  static final ClassNode[] NAME_OVERRIDING_TYPES = [EMBEDDED_TYPE, PROPERTY_TYPE, REFERENCE_TYPE, SERIALIZED_TYPE] as ClassNode[]
  static final ClassNode[] CANNOT_QUERY_PAST_TYPES = [REFERENCE_TYPE, SERIALIZED_TYPE] as ClassNode[]
  static final String MONGO_ID_FIELD_NAME = '_id'

  /**
   * Attempts to resolve a mongo style field query and returns a FieldQueryResult containing the type of the result if it is able
   * to be resolved, or an error message otherwise.
   * @param entityType the classNode from which to begin resolving the query from
   * @param queryString the mongo style query String
   * @return a FieldQueryResult containing the field query result type if it is able to be resolved, and an error message otherwise.
   */
  FieldQueryResult resolve(ClassNode entityType, String queryString) {
    ClassNode ownerType = entityType
    String[] fieldNames = queryString.split('\\.')
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
        return cannotQueryPastResult(field, previousOwnerType, cannotQueryPastAnnotation)
      }

      String fieldName = fieldNames[index++]
      field = ownerType.getField(fieldName) ?: findFieldByOverridingName(ownerType, fieldName)

      if (!field || field.isStatic() || isFieldTransient(field)) {
        return noPersistedFieldResult(fieldName, ownerType)
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
    return new FieldQueryResult(type: field?.type ?: ownerType)
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

  static FieldQueryResult noPersistedFieldResult(String fieldName, ClassNode ownerType) {
    return new FieldQueryResult(error: "No such persisted field: $fieldName for class: ${ownerType.getName()}".toString())
  }

  static FieldQueryResult cannotQueryPastResult(FieldNode field, ClassNode ownerType, AnnotationNode cannotQueryPastAnnotation) {
    return new FieldQueryResult(error: "Cannot access fields of ${ownerType.name}.$field.name since it is annotated with @${cannotQueryPastAnnotation.classNode.name}".toString())
  }

  static class FieldQueryResult {
    ClassNode type
    String error
  }
}
