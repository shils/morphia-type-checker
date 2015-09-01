package com.shils.morphia

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer


class MorphiaTypeCheckingCustomizerTest extends GroovyShellTestCase {

  @Override
  GroovyShell createNewShell() {
    def config = new CompilerConfiguration()
    def ic = new ImportCustomizer().addImports(
            'org.mongodb.morphia.annotations.Entity',
            'groovy.transform.CompileStatic',
            'groovy.transform.TypeChecked',
            'groovy.transform.TypeCheckingMode',
            'groovy.transform.ASTTest',
            'org.codehaus.groovy.ast.ClassHelper',
            'org.bson.types.ObjectId',
            'org.mongodb.morphia.dao.BasicDAO'
    )
    ic.addStaticStars('org.codehaus.groovy.control.CompilePhase', 'com.shils.morphia.MorphiaTypeCheckingCustomizer')
    config.addCompilationCustomizers(ic, new MorphiaTypeCheckingCustomizer())
    new GroovyShell(config)
  }

  void testCustomizerForEntityWithTypeChecked() {
    shell.evaluate '''
      @Entity
      @TypeChecked
      @ASTTest(phase = CANONICALIZATION, value = {
        def extensionsExpr = node.getAnnotations(TYPE_CHECKED_TYPE).first().getMember('extensions').expressions.first()
        assert extensionsExpr.value == 'com.shils.morphia.EntityTypeCheckingExtension'
      })
      class A {}
      null
    '''

    shell.evaluate '''
      @Entity
      @TypeChecked(TypeCheckingMode.SKIP)
      @ASTTest(phase = CANONICALIZATION, value = {
        assert !node.getAnnotations(TYPE_CHECKED_TYPE).first().getMember('extensions')
      })
      class A {}
      null
    '''
  }

  void testCustomizerForEntityWithCompileStatic() {
    shell.evaluate '''
      @Entity
      @CompileStatic
      @ASTTest(phase = CANONICALIZATION, value = {
        def extensionsExpr = node.getAnnotations(COMPILE_STATIC_TYPE).first().getMember('extensions').expressions.first()
        assert extensionsExpr.value == 'com.shils.morphia.EntityTypeCheckingExtension'
      })
      class A {}
      null
    '''

    shell.evaluate '''
      @Entity
      @CompileStatic(TypeCheckingMode.SKIP)
      @ASTTest(phase = CANONICALIZATION, value = {
        assert !node.getAnnotations(COMPILE_STATIC_TYPE).first().getMember('extensions')
      })
      class A {}
      null
    '''
  }

  void testCustomizerForDAOWithTypeChecked() {
    shell.evaluate '''
      @Entity
      class A {}

      @TypeChecked
      @ASTTest(phase = CANONICALIZATION, value = {
        def extensionsExpr = node.getAnnotations(TYPE_CHECKED_TYPE).first().getMember('extensions').expressions.first()
        assert extensionsExpr.value == 'com.shils.morphia.DAOTypeCheckingExtension'
      })
      class ADao extends BasicDAO<A, ObjectId> {}
      null
    '''

    shell.evaluate '''
      @Entity
      class A {}

      @TypeChecked(TypeCheckingMode.SKIP)
      @ASTTest(phase = CANONICALIZATION, value = {
        assert !node.getAnnotations(TYPE_CHECKED_TYPE).first().getMember('extensions')
      })
      class ADao extends BasicDAO<A, ObjectId> {}
      null
    '''
  }

  void testCustomizerForDAOWithCompileStatic() {
    shell.evaluate '''
      @Entity
      class A {}

      @CompileStatic
      @ASTTest(phase = CANONICALIZATION, value = {
        def extensionsExpr = node.getAnnotations(COMPILE_STATIC_TYPE).first().getMember('extensions').expressions.first()
        assert extensionsExpr.value == 'com.shils.morphia.DAOTypeCheckingExtension'
      })
      class ADao extends BasicDAO<A, ObjectId> {}
      null
    '''

    shell.evaluate '''
      @Entity
      class A {}

      @CompileStatic(TypeCheckingMode.SKIP)
      @ASTTest(phase = CANONICALIZATION, value = {
        assert !node.getAnnotations(COMPILE_STATIC_TYPE).first().getMember('extensions')
      })
      class ADao extends BasicDAO<A, ObjectId> {}
      null
    '''
  }
}