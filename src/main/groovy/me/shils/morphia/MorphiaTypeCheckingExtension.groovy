package me.shils.morphia

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.transform.stc.AbstractTypeCheckingExtension
import me.shils.morphia.MorphiaFieldQueryResolver.FieldQueryResult
import me.shils.morphia.MorphiaFieldQueryResolver.QueryErrorResult
import me.shils.morphia.MorphiaFieldQueryResolver.ResolvedFieldResult

import static org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport.implementsInterfaceOrIsSubclassOf

/**
 *
 * @author Shil Sinha
 */
@InheritConstructors
@CompileStatic
abstract class MorphiaTypeCheckingExtension extends AbstractTypeCheckingExtension {

  private static final ClassNode COLLECTION_TYPE = ClassHelper.make(Collection.class)

  protected MorphiaFieldQueryResolver fieldQueryResolver = new MorphiaFieldQueryResolver()

  abstract ClassNode currentEntityType()

  /**
   * Resolves a mongo style field query and returns the type of the result
   * @param queryString the mongo style query String
   * @param argumentNode the ASTNode representing the query String
   * @return the type of the field query result, or null if there is a validation error
   */
  protected ClassNode resolveFieldQuery(String queryString, ASTNode argumentNode) {
    FieldQueryResult result = fieldQueryResolver.resolve(currentEntityType(), queryString)
    if (result instanceof QueryErrorResult) {
      addStaticTypeError(((QueryErrorResult) result).error, argumentNode)
      return null
    }
    return ((ResolvedFieldResult) result).type
  }

  protected void validateArrayFieldQuery(String queryString, ASTNode argumentNode) {
    ClassNode fieldType = resolveFieldQuery(queryString, argumentNode)
    if (fieldType && !implementsInterfaceOrIsSubclassOf(fieldType, COLLECTION_TYPE) && !fieldType.isArray()){
      addStaticTypeError("$queryString does not refer to an array field", argumentNode)
    }
  }

  protected void validateNumericFieldQuery(String queryString, ASTNode argumentNode) {
    ClassNode fieldType = resolveFieldQuery(queryString, argumentNode)
    if (fieldType && !implementsInterfaceOrIsSubclassOf(ClassHelper.getWrapper(fieldType), ClassHelper.Number_TYPE)){
      addStaticTypeError("$queryString does not refer to a numeric field", argumentNode)
    }
  }
}