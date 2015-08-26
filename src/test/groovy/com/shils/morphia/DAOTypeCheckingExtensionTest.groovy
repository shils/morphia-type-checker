package com.shils.morphia

import groovy.transform.CompileStatic
import org.bson.types.ObjectId
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Entity
import org.mongodb.morphia.annotations.Id
import org.mongodb.morphia.annotations.Property;

class DAOTypeCheckingExtensionTest extends GroovyShellTestCase {

  @Override
  GroovyShell createNewShell() {
    def config = new CompilerConfiguration()
    def ic = new ImportCustomizer().addImports(
            'org.mongodb.morphia.dao.BasicDAO',
            'org.mongodb.morphia.Key',
            'org.bson.types.ObjectId',
            'groovy.transform.CompileStatic',
            'com.shils.morphia.A',
            'com.shils.morphia.B'
    )
    def asttc = new ASTTransformationCustomizer(CompileStatic, extensions: ['com.shils.morphia.DAOTypeCheckingExtension'])
    config.addCompilationCustomizers(ic, asttc)
    new GroovyShell(config)
  }

  void testIncorrectFieldQueryShouldFail(){
    def message = shouldFail '''
      class ADao extends BasicDAO<A, ObjectId> {

        A aQuery(int anInt) {
          findOne(createQuery().field('aInt').equal(anInt))
        }
      }
      null
    '''
    assert message.contains('No such persisted field: aInt for class: com.shils.morphia.A')
  }

  void testEmbeddedFieldQuery(){
    shell.evaluate '''
      class ADao extends BasicDAO<A, ObjectId> {

        A anEmbeddedQuery(String embeddedAString) {
          findOne(createQuery().field('embedded.aString').equal(embeddedAString))
        }
      }
      null
    '''
  }

  void testPropertyAnnotatedFieldQuery(){
    shell.evaluate '''
      class ADao extends BasicDAO<A, ObjectId> {

        A aQuery(String aStringProperty) {
          findOne(createQuery().field('aString').equal(aStringProperty))
        }
      }
      null
    '''
  }

  void testPropertyAnnotatedEmbeddedFieldQuery(){
    shell.evaluate '''
      class ADao extends BasicDAO<A, ObjectId> {

        A aQuery(String aStringProperty) {
          findOne(createQuery().field('embedded.anotherInt').equal(aStringProperty))
        }

      }
      null
    '''
  }

  void testIncorrectFilterQueryShouldFail(){
    def message = shouldFail '''
      class ADao extends BasicDAO<A, ObjectId> {

        A aQuery(int anInt) {
          findOne(createQuery().filter('aInt >', anInt))
        }
      }
      null
    '''
    assert message.contains('No such persisted field: aInt for class: com.shils.morphia.A')
  }

  void testIncorrectOrderQueryShouldFail(){
    def message = shouldFail '''
      class ADao extends BasicDAO<A, ObjectId> {

        A aQuery(int anInt) {
          findOne(createQuery().order('aInt'))
        }

        A aQueryDesc(int anInt) {
          findOne(createQuery().order('-descAInt'))
        }
      }
      null
    '''
    assert message.contains('No such persisted field: aInt for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: descAInt for class: com.shils.morphia.A')
  }

  void testIncorrectFieldArgsInUpdateOpsMethodCallsShouldFail() {
    def message = shouldFail '''
      class ADao extends BasicDAO<A, ObjectId> {

        void setAnInt(A a, int anInt) {
          update(createQuery(), createUpdateOperations().set('setInt', anInt))
        }

        void setAnIntOnInsert(A a, int anInt){
          update(createQuery(), createUpdateOperations().setOnInsert('setOnInsertInt', anInt))
        }

        void unsetAnInt() {
          update(createQuery(), createUpdateOperations().unset('unsetInt'))
        }

        void decAnInt() {
          update(createQuery(), createUpdateOperations().dec('decInt'))
        }

        void incAnInt() {
          update(createQuery(), createUpdateOperations().inc('incInt'))
        }

        void maxAnInt(int anInt) {
          update(createQuery(), createUpdateOperations().max('maxInt', anInt))
        }

        void minAnInt(int anInt) {
          update(createQuery(), createUpdateOperations().dec('minInt', anInt))
        }

        void addToAList(int anInt) {
          update(createQuery(), createUpdateOperations().add('addList', anInt))
        }

        void addAllToAList(List ints) {
          update(createQuery(), createUpdateOperations().addAll('addAllList', ints))
        }

        void removeFirstFromAList() {
          update(createQuery(), createUpdateOperations().removeFirst('removeFirstList'))
        }

        void removeLastFromAList() {
          update(createQuery(), createUpdateOperations().removeLast('removeLastList'))
        }

        void removeAllFromAList(int anInt) {
          update(createQuery(), createUpdateOperations().removeAll('removeAllList'))
        }

      }
      null
    '''
    assert message.contains('No such persisted field: setInt for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: setOnInsertInt for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: unsetInt for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: decInt for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: incInt for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: maxInt for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: minInt for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: addList for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: addAllList for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: removeFirstList for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: removeLastList for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: removeAllList for class: com.shils.morphia.A')
  }

  void testNonArrayFieldUsedForArrayUpdateShouldFail(){
    def message = shouldFail '''
      class ADao extends BasicDAO<A, ObjectId> {

        void addToAList(int anInt) {
          update(createQuery(), createUpdateOperations().add('anInt', anInt))
        }

        void addAllToAList(List<Integer> aList) {
          update(createQuery(), createUpdateOperations().addAll('anInt', aList))
        }

        void removeFirstFromAList() {
          update(createQuery(), createUpdateOperations().removeFirst('anInt'))
        }

        void removeLastFromAList() {
          update(createQuery(), createUpdateOperations().removeLast('anInt'))
        }

        void removeAllFromAList(int anInt) {
          update(createQuery(), createUpdateOperations().removeAll('anInt', anInt))
        }
      }
      null
    '''
    assert message.contains('anInt does not refer to an array field')
  }

  void testNonNumericFieldUsedForNumericUpdateShouldFail(){
    def message = shouldFail '''
      class ADao extends BasicDAO<A, ObjectId> {

        void decAnInt() {
          update(createQuery(), createUpdateOperations().dec('aString'))
        }

        void incAnInt() {
          update(createQuery(), createUpdateOperations().inc('aString'))
        }

        void maxAnInt(int anInt) {
          update(createQuery(), createUpdateOperations().max('aString', anInt))
        }

        void minAnInt(int anInt) {
          update(createQuery(), createUpdateOperations().min('aString', anInt))
        }
      }
      null
    '''
    assert message.contains('aString does not refer to a numeric field')
  }

  void testIncorrectFieldArgsInDAOMethodCallsShouldFail(){
    def message = shouldFail '''
      class ADao extends BasicDAO<A, ObjectId> {

        List<ObjectId> findAIds(int anInt) {
          findIds('findAIds', anInt)
        }

        Key<A> findOneAId(int anInt) {
          findOneId('findOneAId', anInt)
        }

        boolean existsA(int anInt) {
          exists('existsA', anInt)
        }

        long countA(int anInt) {
          count('countA', anInt)
        }

        A findOneA(int anInt) {
          findOne('findOneA', anInt)
        }
      }
      null
    '''
    assert message.contains('No such persisted field: findAIds for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: findOneAId for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: existsA for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: countA for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: findOneA for class: com.shils.morphia.A')
  }

  void testEmbeddedArrayFieldQuery() {
    shell.evaluate '''
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.morphia.A

      @CompileStatic(extensions = ['com.shils.morphia.DAOTypeCheckingExtension'])
      class ADao extends BasicDAO<A, ObjectId> {

        A anEmbeddedArrayQuery(String embeddedAString) {
          findOne(createQuery().field('embeddedArray.aString').equal(embeddedAString))
        }

        A anEmbeddedCollectionQuery(String embeddedAString) {
          findOne(createQuery().field('embeddedCollection.aString').equal(embeddedAString))
        }
      }
      null
    '''

    def message = shouldFail '''
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.morphia.A

      @CompileStatic(extensions = ['com.shils.morphia.DAOTypeCheckingExtension'])
      class ADao extends BasicDAO<A, ObjectId> {

        A anEmbeddedArrayQuery(String embeddedAString) {
          findOne(createQuery().field('embeddedArray.arrayString').equal(embeddedAString))
        }

        A anEmbeddedCollectionQuery(String embeddedAString) {
          findOne(createQuery().field('embeddedCollection.collectionString').equal(embeddedAString))
        }
      }
      null
    '''
    assert message.contains('No such persisted field: arrayString for class: com.shils.morphia.B')
    assert message.contains('No such persisted field: collectionString for class: com.shils.morphia.B')
  }

  void testEmbeddedBoundedWCCollectionFieldQuery() {
    shell.evaluate '''
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.morphia.A

      @CompileStatic(extensions = ['com.shils.morphia.DAOTypeCheckingExtension'])
      class ADao extends BasicDAO<A, ObjectId> {

        A anEmbeddedBoundedWCCollectionQuery(String embeddedAString) {
          findOne(createQuery().field('embeddedBoundedWCCollection.aString').equal(embeddedAString))
        }
      }
      null
    '''

    def message = shouldFail '''
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.morphia.A

      @CompileStatic(extensions = ['com.shils.morphia.DAOTypeCheckingExtension'])
      class ADao extends BasicDAO<A, ObjectId> {

        A anEmbeddedBoundedWCCollectionQuery(String embeddedAString) {
          findOne(createQuery().field('embeddedBoundedWCCollection.aStrin').equal(embeddedAString))
        }
      }
      null
    '''
    assert message.contains('No such persisted field: aStrin for class: com.shils.morphia.B')
  }

  void testEmbeddedMapFieldQuery() {
    shell.evaluate '''
      class ADao extends BasicDAO<A, ObjectId> {

        A anEmbeddedMapQuery(String embeddedAString) {
          findOne(createQuery().field('embeddedMap.foo.aString').equal(embeddedAString))
        }
      }
      null
    '''

    def message = shouldFail '''
      class ADao extends BasicDAO<A, ObjectId> {

        A anEmbeddedMapQuery(String embeddedAString) {
          findOne(createQuery().field('embeddedMap.foo.aStrin').equal(embeddedAString))
        }
      }
      null
    '''
    assert message.contains('No such persisted field: aStrin for class: com.shils.morphia.B')
  }

  void testEmbeddedBoundedWCMapFieldQuery() {
    shell.evaluate '''
      class ADao extends BasicDAO<A, ObjectId> {

        A anEmbeddedBoundedWCMapQuery(String embeddedAString) {
          findOne(createQuery().field('embeddedBoundedWCMap.foo.aString').equal(embeddedAString))
        }
      }
      null
    '''

    def message = shouldFail '''
      class ADao extends BasicDAO<A, ObjectId> {

        A anEmbeddedBoundedWCMapQuery(String embeddedAString) {
          findOne(createQuery().field('embeddedBoundedWCMap.foo.aStrin').equal(embeddedAString))
        }
      }
      null
    '''
    assert message.contains('No such persisted field: aStrin for class: com.shils.morphia.B')
  }

  @Override
  String shouldFail(String script) {
    shouldFail {
      shell.evaluate(script)
    }
  }
}

@Entity
class A {
  @Id
  ObjectId id
  int anInt
  @Property('aString')
  String aStringProperty
  B embedded
  B[] embeddedArray
  Collection<B> embeddedCollection
  Collection<? extends B> embeddedBoundedWCCollection
  Map<String, B> embeddedMap
  Map<String, ? extends B> embeddedBoundedWCMap
}

@Embedded
class B {
  String aString
  @Property('anotherInt')
  int anotherIntProperty
}
