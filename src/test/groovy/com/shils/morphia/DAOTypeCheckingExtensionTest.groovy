package com.shils.morphia

import groovy.transform.CompileStatic
import org.bson.types.ObjectId
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Entity
import org.mongodb.morphia.annotations.Id
import org.mongodb.morphia.annotations.Property
import org.mongodb.morphia.annotations.Transient;

class DAOTypeCheckingExtensionTest extends GroovyShellTestCase {

  @Override
  GroovyShell createNewShell() {
    def config = new CompilerConfiguration()
    def ic = new ImportCustomizer().addImports(
            'org.mongodb.morphia.dao.BasicDAO',
            'org.mongodb.morphia.Key',
            'org.mongodb.morphia.query.Query',
            'org.mongodb.morphia.query.QueryImpl',
            'org.mongodb.morphia.query.UpdateOperations',
            'org.mongodb.morphia.query.UpdateOpsImpl',
            'org.mongodb.morphia.Datastore',
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

  void testNonConstantFieldArgsInQueryMethodCallsShouldNotFail() {
    shell.evaluate '''
      class ADao extends BasicDAO<A, ObjectId> {

        A aQuery(String aFieldName, int anInt) {
          findOne(createQuery().field(aFieldName).equal(anInt))
        }

        A anInterpolatedQuery(String aFieldName, int anInt) {
          findOne(createQuery().field("$aFieldName").equal(anInt))
        }
      }
      null
    '''
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

  void testNonConstantFieldArgsInUpdateOpsMethodCallsShouldNotFail() {
    shell.evaluate '''
      class ADao extends BasicDAO<A, ObjectId> {

        void anUpdate(String aFieldName, int anInt) {
          update(createQuery(), createUpdateOperations().set(aFieldName, anInt))
        }

        void anInterpolatedUpdate(String aFieldName, int anInt) {
          update(createQuery(), createUpdateOperations().set("$aFieldName", anInt))
        }
      }
      null
    '''
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

  void testTransientFieldQueriesShouldFail() {
    def message = shouldFail '''
      class ADao extends BasicDAO<A, ObjectId> {

        A aTransientIntQuery(int anInt) {
          findOne(createQuery().field('aTransientInt').equal(anInt))
        }

        A aMorphiaTransientIntQuery(int anInt) {
          findOne(createQuery().field('aMorphiaTransientInt').equal(anInt))
        }
      }
      null
    '''
    assert message.contains('No such persisted field: aTransientInt for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: aMorphiaTransientInt for class: com.shils.morphia.A')
  }

  void testDisableValidationAwarenessInQueries() {
    shell.evaluate '''
      class ADao extends BasicDAO<A, ObjectId> {

        A aNonValidatedQuery(int anInt) {
          findOne(createQuery().disableValidation().field('notValidated').equal(anInt))
        }
      }
      null
    '''
  }

  void testEnableValidationAwarenessInQueries() {
    def message = shouldFail '''
      class ADao extends BasicDAO<A, ObjectId> {

        A aPartiallyValidatedQuery(int anInt, String aString) {
          findOne(createQuery().disableValidation().field('notValidated').equal(anInt).
                  enableValidation().field('validated').equal(aString)
          )
        }
      }
      null
    '''
    assert message.contains('No such persisted field: validated for class: com.shils.morphia.A')
    assert !message.contains('notValidated')
  }

  void testDisableValidationAwarenessInUpdateOps() {
    shell.evaluate '''
      class ADao extends BasicDAO<A, ObjectId> {

        void aNonValidatedUpdate(int anInt) {
          update(createQuery(), createUpdateOperations().disableValidation().set('notValidated', anInt))
        }
      }
      null
    '''
  }

  void testEnableValidationAwarenessInUpdateOps() {
    def message = shouldFail '''
      class ADao extends BasicDAO<A, ObjectId> {

        void aPartiallyValidatedUpdate(int anInt, String aString) {
          update(createQuery(), createUpdateOperations().disableValidation().set('notValidated', anInt).
                  enableValidation().set('validated', aString)
          )
        }
      }
      null
    '''
    assert message.contains('No such persisted field: validated for class: com.shils.morphia.A')
    assert !message.contains('notValidated')
  }

  void testValidationOfStaticMethodCalls() {
    def message = shouldFail '''
      class ADao extends BasicDAO<A, ObjectId> {

        A aQuery(int anInt) {
          findOne(createQueryStatic(getDatastore()).field('aInt').equal(anInt))
        }

        void anUpdate(String aString) {
          update(createQuery(), createUpdateOpsStatic(getDatastore()).set('aStrin', aString))
        }

        static Query<A> createQueryStatic(Datastore ds) {
          ds.createQuery(A.class)
        }

        static UpdateOperations<A> createUpdateOpsStatic(Datastore ds) {
          ds.createUpdateOperations(A.class)
        }
      }
      null
    '''
    assert message.contains('No such persisted field: aInt for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: aStrin for class: com.shils.morphia.A')
  }

  void testValidationOfConstructorCalls() {
    def message = shouldFail '''
      class ADao extends BasicDAO<A, ObjectId> {

        A aQuery(int anInt) {
          Query query = new QueryImpl(A.class, getDatastore().getCollection(), getDatastore()).field('aInt').equal(anInt)
          findOne(query)
        }

        void anUpdate(String aString) {
          UpdateOperations ops = new UpdateOpsImpl(A.class, getDatastore().getMapper()).set('aStrin', aString)
          update(createQuery(), ops)
        }
      }
      null
    '''
    assert message.contains('No such persisted field: aInt for class: com.shils.morphia.A')
    assert message.contains('No such persisted field: aStrin for class: com.shils.morphia.A')
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
  transient int aTransientInt
  @Transient
  int aMorphiaTransientInt
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
