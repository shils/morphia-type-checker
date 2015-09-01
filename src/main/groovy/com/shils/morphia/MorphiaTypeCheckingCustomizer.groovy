package com.shils.morphia

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.customizers.CompilationCustomizer
import org.mongodb.morphia.annotations.Entity
import org.mongodb.morphia.dao.DAO

@CompileStatic
class MorphiaTypeCheckingCustomizer extends CompilationCustomizer {

  static final ClassNode DAO_TCE_TYPE = ClassHelper.make(DAOTypeCheckingExtension.class)
  static final String DAO_TCE_TYPE_NAME = DAO_TCE_TYPE.name
  static final ClassNode ENTITY_TCE_TYPE = ClassHelper.make(EntityTypeCheckingExtension.class)
  static final String ENTITY_TCE_TYPE_NAME = ENTITY_TCE_TYPE.name

  static final ClassNode DAO_TYPE = ClassHelper.make(DAO.class)
  static final ClassNode ENTITY_TYPE = ClassHelper.make(Entity.class)
  static final ClassNode TYPE_CHECKED_TYPE = ClassHelper.make(TypeChecked.class)
  static final ClassNode COMPILE_STATIC_TYPE = ClassHelper.make(CompileStatic.class)
  static final ClassNode[] STC_TYPES = [TYPE_CHECKED_TYPE, COMPILE_STATIC_TYPE] as ClassNode[]
  private static final String SKIP_STRING = TypeCheckingMode.SKIP.toString()

  MorphiaTypeCheckingCustomizer() {
    super(CompilePhase.CANONICALIZATION)
  }

  @Override
  void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
    AnnotationNode stcAnnotation = classNode.getAnnotations().find {
      STC_TYPES.contains(it.classNode)
    }
    if (!stcAnnotation || isSkipMode(stcAnnotation))
      return

    ListExpression extensions = (ListExpression) stcAnnotation.getMember('extensions') ?: new ListExpression()
    if (classNode.implementsInterface(DAO_TYPE)) {
      extensions.addExpression(new ConstantExpression(DAO_TCE_TYPE_NAME))
    } else if (classNode.getAnnotations(ENTITY_TYPE)) {
      extensions.addExpression(new ConstantExpression(ENTITY_TCE_TYPE_NAME))
    } else {
      return
    }
    stcAnnotation.setMember('extensions', extensions)
  }

  private static boolean isSkipMode(AnnotationNode stcAnnotation) {
    Expression typeCheckingMode = stcAnnotation.getMember('value')
    return (typeCheckingMode instanceof ConstantExpression && SKIP_STRING == ((ConstantExpression) typeCheckingMode).value.toString()) ||
            (typeCheckingMode instanceof PropertyExpression && SKIP_STRING == ((PropertyExpression)typeCheckingMode).propertyAsString)
  }
}
