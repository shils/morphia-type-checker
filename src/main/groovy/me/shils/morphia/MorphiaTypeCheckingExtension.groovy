package me.shils.morphia

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
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
abstract class MorphiaTypeCheckingExtension extends AbstractTypeCheckingExtension {

  static final ClassNode COLLECTION_TYPE = ClassHelper.make(Collection.class)

  protected MorphiaFieldAccessResolver fieldAccessResolver = new MorphiaFieldAccessResolver()

  abstract ClassNode currentEntityType()

  /**
   * Resolves a chain of field accesses and returns the type of the result
   * @param fieldArgument the field access chain String
   * @param argumentNode the ASTNode representing the field access chain String
   * @return the type of the result of the chain of field accesses, or null if there is a validation error
   */
  ClassNode resolveFieldArgument(String fieldArgument, ASTNode argumentNode) {
    List<String> errorMessages = []
    ClassNode resultType = fieldAccessResolver.resolve(currentEntityType(), fieldArgument, errorMessages)
    for (String msg: errorMessages) {
      addStaticTypeError(msg, argumentNode)
    }
    return resultType
  }

  void validateArrayFieldArgument(String fieldArgument, ASTNode argumentNode) {
    ClassNode fieldType = resolveFieldArgument(fieldArgument, argumentNode)
    if (fieldType && !implementsInterfaceOrIsSubclassOf(fieldType, COLLECTION_TYPE) && !fieldType.isArray()){
      addStaticTypeError("$fieldArgument does not refer to an array field", argumentNode)
    }
  }

  void validateNumericFieldArgument(String fieldArgument, ASTNode argumentNode) {
    ClassNode fieldType = resolveFieldArgument(fieldArgument, argumentNode)
    if (fieldType && !implementsInterfaceOrIsSubclassOf(ClassHelper.getWrapper(fieldType), ClassHelper.Number_TYPE)){
      addStaticTypeError("$fieldArgument does not refer to a numeric field", argumentNode)
    }
  }
}