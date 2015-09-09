package me.shils.morphia

import org.bson.types.ObjectId
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Entity
import org.mongodb.morphia.annotations.Id
import org.mongodb.morphia.annotations.Property

class MorphiaFieldQueryResolverTest extends GroovyShellTestCase {

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
            'me.shils.morphia.MorphiaFieldQueryResolver',
            'me.shils.morphia.EmbeddedClass',
            'me.shils.morphia.ReferencedClass',
            'me.shils.morphia.SerializableClass'
    ).addImport('MorphiaReference', 'org.mongodb.morphia.annotations.Reference')
    config.addCompilationCustomizers(ic)
    new GroovyShell(config)
  }

  void testFindFieldByOverridingName() {
    shell.evaluate '''
      @ASTTest(phase = INSTRUCTION_SELECTION, value = {
        assert MorphiaFieldQueryResolver.findFieldByOverridingName(node, 'embeddedOverride') == node.getField('embedded')
        assert MorphiaFieldQueryResolver.findFieldByOverridingName(node, 'propertyOverride') == node.getField('property')
        assert MorphiaFieldQueryResolver.findFieldByOverridingName(node, 'referenceOverride') == node.getField('reference')
        assert MorphiaFieldQueryResolver.findFieldByOverridingName(node, 'serializedOverride') == node.getField('serialized')
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

  void testQueryingPastSerializedFieldsError() {
    shell.evaluate '''
      @ASTTest(phase = INSTRUCTION_SELECTION, value = {
        def result = new MorphiaFieldQueryResolver().resolve(node, 'serializableClass.aString')
        assert !result.type
        assert result.error == 'Cannot access fields of A.serializableClass since it is annotated with @org.mongodb.morphia.annotations.Serialized'
      })
      @CompileStatic
      class A {
        @Serialized
        SerializableClass serializableClass
      }
      null
    '''
  }

  void testQueryingPastReferenceFieldsError() {
    shell.evaluate '''
      @ASTTest(phase = INSTRUCTION_SELECTION, value = {
        def result = new MorphiaFieldQueryResolver().resolve(node, 'referencedClass.aString')
        assert !result.type
        assert result.error == 'Cannot access fields of A.referencedClass since it is annotated with @org.mongodb.morphia.annotations.Reference'
      })
      @CompileStatic
      class A {
        @MorphiaReference
        ReferencedClass referencedClass
      }
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

@Embedded
class EmbeddedClass {
  String aString
  @Property('anotherInt')
  int anotherIntProperty
}

@Entity
class ReferencedClass {
  @Id
  ObjectId id
  String aString
}

class SerializableClass implements Serializable {
  String aString
}
