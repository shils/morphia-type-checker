package me.shils.morphia


class MorphiaTypeCheckingExtensionTest extends GroovyTestCase {

  void testFindFieldByOverridingName() {
    assertScript '''
      import org.mongodb.morphia.annotations.Entity
      import org.mongodb.morphia.annotations.Embedded
      import org.mongodb.morphia.annotations.Property
      import org.mongodb.morphia.annotations.Reference as MorphiaReference
      import org.mongodb.morphia.annotations.Serialized
      import org.codehaus.groovy.control.CompilePhase
      import groovy.transform.CompileStatic
      import groovy.transform.ASTTest
      import me.shils.morphia.MorphiaTypeCheckingExtension

      @ASTTest(phase = INSTRUCTION_SELECTION, value = {
        assert MorphiaTypeCheckingExtension.findFieldByOverridingName(node, 'embeddedOverride') == node.getField('embedded')
        assert MorphiaTypeCheckingExtension.findFieldByOverridingName(node, 'propertyOverride') == node.getField('property')
        assert MorphiaTypeCheckingExtension.findFieldByOverridingName(node, 'referenceOverride') == node.getField('reference')
        assert MorphiaTypeCheckingExtension.findFieldByOverridingName(node, 'serializedOverride') == node.getField('serialized')
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
}
