package me.shils.morphia

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

class MorphiaFieldAccessResolverTest extends GroovyShellTestCase {

  @Override
  GroovyShell createNewShell() {
    def config = new CompilerConfiguration()
    def ic = new ImportCustomizer().addImports(
            'org.mongodb.morphia.annotations.Entity',
            'org.mongodb.morphia.annotations.Embedded',
            'org.mongodb.morphia.annotations.Property',
            'org.mongodb.morphia.annotations.Serialized',
            'org.codehaus.groovy.control.CompilePhase',
            'groovy.transform.CompileStatic',
            'groovy.transform.ASTTest',
            'me.shils.morphia.MorphiaFieldAccessResolver'
    ).addImport('MorphiaReference', 'org.mongodb.morphia.annotations.Reference')
    config.addCompilationCustomizers(ic)
    new GroovyShell(config)
  }

  void testFindFieldByOverridingName() {
    shell.evaluate '''
      @ASTTest(phase = INSTRUCTION_SELECTION, value = {
        assert MorphiaFieldAccessResolver.findFieldByOverridingName(node, 'embeddedOverride') == node.getField('embedded')
        assert MorphiaFieldAccessResolver.findFieldByOverridingName(node, 'propertyOverride') == node.getField('property')
        assert MorphiaFieldAccessResolver.findFieldByOverridingName(node, 'referenceOverride') == node.getField('reference')
        assert MorphiaFieldAccessResolver.findFieldByOverridingName(node, 'serializedOverride') == node.getField('serialized')
      })
      @Entity
      @CompileStatic
      class A {
        @Embedded('embeddedOverride')
        B embedded

        @Serialized('serializedOverride')
        Date serialized

        @Property('propertyOverride')
        String property

        @MorphiaReference('referenceOverride')
        C reference
      }

      @Embedded
      class B {}

      @Entity
      class C {}
      null
    '''
  }

  @Override
  String shouldFail(String script) {
    shouldFail {
      shell.evaluate(script)
    }
  }
}
