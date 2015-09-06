package me.shils.morphia

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.AnnotationConstantExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.runtime.StringGroovyMethods
import org.mongodb.morphia.annotations.Entity
import org.mongodb.morphia.annotations.Index
import org.mongodb.morphia.annotations.Indexes

/**
 * A type checking extension for classes annotated with {@link org.mongodb.morphia.annotations.Entity}.
 *
 * @author Shil Sinha
 */
@CompileStatic
@InheritConstructors
class EntityTypeCheckingExtension extends MorphiaTypeCheckingExtension {

  static final ClassNode INDEX_TYPE = ClassHelper.make(Index)
  static final ClassNode INDEXES_TYPE = ClassHelper.make(Indexes)
  static final ClassNode ENTITY_TYPE = ClassHelper.make(Entity)

  private ClassNode entityType

  @Override
  ClassNode currentEntityType() {
    return entityType
  }

  @Override
  void afterVisitClass(ClassNode classNode) {
    if (!isAnnotatedBy(classNode, ENTITY_TYPE))
      return

    entityType = classNode
    List<AnnotationNode> annotations = entityType.getAnnotations()
    for (AnnotationNode annotation: annotations) {
      if (INDEXES_TYPE.equals(annotation.classNode)) {
        validateIndexesAnnotation(annotation)
      }
    }
  }

  private void validateIndexesAnnotation(AnnotationNode indexesNode) {
    Expression member = indexesNode.getMember('value')
    List<Expression> indexExpressions = member instanceof ListExpression ? ((ListExpression) member).expressions : [member]
    for (Expression e: indexExpressions) {
      validateIndexAnnotation((AnnotationNode) ((AnnotationConstantExpression) e).value)
    }
  }

  private void validateIndexAnnotation(AnnotationNode indexNode) {
    Expression member = indexNode.getMember('fields')
    if (member) {
      List<Expression> fieldExpressions = member instanceof ListExpression ? ((ListExpression) member).expressions : [member]
      for (Expression e: fieldExpressions) {
        validateFieldAnnotation((AnnotationNode) ((AnnotationConstantExpression) e).value)
      }
      return
    }
    //must be using deprecated String value()
    member = indexNode.getMember('value')
    if (member) {
      validateFieldArguments((ConstantExpression) member)
    }
  }

  private void validateFieldAnnotation(AnnotationNode fieldAnnotationNode) {
    ConstantExpression member = (ConstantExpression) fieldAnnotationNode.getMember('value')
    resolveFieldArgument((String) member.value, member)
  }

  private void validateFieldArguments(ConstantExpression argsExpression) {
    CharSequence argsString = (CharSequence) argsExpression.value
    List<String> fieldArguments = StringGroovyMethods.tokenize(argsString, ', -')
    for (String arg: fieldArguments) {
      resolveFieldArgument(arg, argsExpression)
    }
  }

}