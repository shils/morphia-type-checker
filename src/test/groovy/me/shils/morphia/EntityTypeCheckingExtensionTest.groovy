package me.shils.morphia

import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer

class EntityTypeCheckingExtensionTest extends GroovyShellTestCase {

  @Override
  GroovyShell createNewShell() {
    def config = new CompilerConfiguration()
    def ic = new ImportCustomizer().addImports(
            'org.mongodb.morphia.annotations.Entity',
            'org.mongodb.morphia.annotations.Embedded',
            'org.mongodb.morphia.annotations.Indexes',
            'org.mongodb.morphia.annotations.Index',
            'org.mongodb.morphia.annotations.IndexOptions',
            'org.mongodb.morphia.annotations.Field',
            'groovy.transform.CompileStatic',
    )
    def asttc = new ASTTransformationCustomizer(CompileStatic, extensions: ['me.shils.morphia.EntityTypeCheckingExtension'])
    config.addCompilationCustomizers(ic, asttc)
    new GroovyShell(config)
  }

  void testIncorrectFieldsInIndexesShouldFail() {
    def message = shouldFail '''
      @Indexes(@Index(fields = @Field('aInt')))
      @Entity
      class A {
        int anInt
      }
    '''
    assert message.contains('No such persisted field: aInt for class: A')

    message = shouldFail '''
      @Indexes(@Index(fields = [@Field('anInt'), @Field('anString')]))
      @Entity
      class A {
        int anInt
        String aString
      }
    '''
    assert message.contains('No such persisted field: anString for class: A')

    message = shouldFail '''
      @Indexes([@Index(fields = @Field('aInt'))])
      @Entity
      class A {
        int anInt
      }
    '''
    assert message.contains('No such persisted field: aInt for class: A')

    message = shouldFail '''
      @Indexes([@Index(fields = [@Field('anInt'), @Field('anString')])])
      @Entity
      class A {
        int anInt
        String aString
      }
    '''
    assert message.contains('No such persisted field: anString for class: A')
  }

  void testIncorrectFieldsInIndexesShouldFailOld() {
    def message = shouldFail '''
      @Indexes([@Index('aInt')])
      @Entity
      class A {
        int anInt
      }
    '''
    assert message.contains('No such persisted field: aInt for class: A')

    message = shouldFail '''
      @Indexes(@Index('-aInt'))
      @Entity
      class A {
        int anInt
      }
    '''
    assert message.contains('No such persisted field: aInt for class: A')

    message = shouldFail '''
      @Indexes([@Index('anInt, -anString')])
      @Entity
      class A {
        int anInt
        String aString
      }
    '''
    assert message.contains('No such persisted field: anString for class: A')

    message = shouldFail '''
      @Indexes(@Index('-anInt, anString'))
      @Entity
      class A {
        int anInt
        String aString
      }
    '''
    assert message.contains('No such persisted field: anString for class: A')
  }

  void testEntityWithInnerEnumShouldNotFail() {
    shell.evaluate '''
      @Entity
      @Indexes(@Index('b'))
      class A {
        B b

        enum B {
          FOO, BAR
        }
      }
      null
    '''
  }

  void testEntityWithInnerStaticClassShouldNotFail() {
    shell.evaluate '''
      @Entity
      @Indexes(@Index('b'))
      class A {
        B b

        static class B {
          int bInt
        }
      }
      null
    '''
  }

  void testIndexWithDisableValidationShouldNotFail() {
    shell.evaluate '''
      @Entity
      @Indexes(@Index(fields = [@Field('notAField')], options = @IndexOptions(disableValidation = true)))
      class A {
      }
      null
    '''
  }

  void testIndexWithDisableValidationShouldNotFailOld() {
    shell.evaluate '''
      @Entity
      @Indexes(@Index(value = 'notAField', disableValidation = true))
      class A {
      }
      null
    '''
  }

  void testIndexOptionsDisableValidationPreferredToIndexDisableValidation() {
    def message = shouldFail '''
      @Entity
      @Indexes(@Index(value = 'notAField', disableValidation = true, options = @IndexOptions(disableValidation = false)))
      class A {
      }
      null
    '''
    assert message.contains('No such persisted field: notAField for class: A')
  }

  @Override
  String shouldFail(String script) {
    shouldFail {
      shell.evaluate(script)
    }
  }
}