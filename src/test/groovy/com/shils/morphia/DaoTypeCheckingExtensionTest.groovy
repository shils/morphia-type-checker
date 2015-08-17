package com.shils.morphia

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Entity
import org.mongodb.morphia.annotations.Id
import org.mongodb.morphia.annotations.Property;

class DaoTypeCheckingExtensionTest extends GroovyTestCase {

  void testIncorrectFieldQueryShouldFail(){
    def message = shouldFail '''
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.morphia.A

      @CompileStatic(extensions = ['com.shils.morphia.DaoTypeCheckingExtension'])
      class ADao extends BasicDAO<A, ObjectId> {

        A aQuery(int anInt) {
          findOne(createQuery().field('aInt').equal(anInt))
        }

      }
      null
    '''
    assert message.contains('No such persisted field: aInt for class: com.shils.morphia.A')
  }

  void testEmbeddedFieldQueryShouldNotFail(){
    assertScript '''
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.morphia.A

      @CompileStatic(extensions = ['com.shils.morphia.DaoTypeCheckingExtension'])
      class ADao extends BasicDAO<A, ObjectId> {

        A anEmbeddedQuery(String embeddedAString) {
          findOne(createQuery().field('embedded.aString').equal(embeddedAString))
        }

      }
      null
    '''
  }

  void testPropertyAnnotatedFieldQueryShouldNotFail(){
    assertScript '''
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.morphia.A

      @CompileStatic(extensions = ['com.shils.morphia.DaoTypeCheckingExtension'])
      class ADao extends BasicDAO<A, ObjectId> {

        A aQuery(String aStringProperty) {
          findOne(createQuery().field('aString').equal(aStringProperty))
        }

      }
      null
    '''
  }

  void testPropertyAnnotatedEmbeddedFieldQueryShouldNotFail(){
    assertScript '''
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.morphia.A

      @CompileStatic(extensions = ['com.shils.morphia.DaoTypeCheckingExtension'])
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
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.morphia.A

      @CompileStatic(extensions = ['com.shils.morphia.DaoTypeCheckingExtension'])
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
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.morphia.A

      @CompileStatic(extensions = ['com.shils.morphia.DaoTypeCheckingExtension'])
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
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.morphia.A

      @CompileStatic(extensions = ['com.shils.morphia.DaoTypeCheckingExtension'])
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
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.morphia.A

      @CompileStatic(extensions = ['com.shils.morphia.DaoTypeCheckingExtension'])
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
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.morphia.A

      @CompileStatic(extensions = ['com.shils.morphia.DaoTypeCheckingExtension'])
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
}

@Entity
class A {
  @Id
  ObjectId id
  int anInt
  @Property('aString')
  String aStringProperty
  B embedded
}

@Embedded
class B {
  String aString
  @Property('anotherInt')
  int anotherIntProperty
}
