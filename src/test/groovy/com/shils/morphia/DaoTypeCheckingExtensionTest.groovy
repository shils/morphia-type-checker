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
          findOne(createQuery().order('aInt', anInt))
        }

      }
      null
    '''
    assert message.contains('No such persisted field: aInt for class: com.shils.morphia.A')

    //test with descending order
    message = shouldFail '''
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.morphia.A

      @CompileStatic(extensions = ['com.shils.morphia.DaoTypeCheckingExtension'])
      class ADao extends BasicDAO<A, ObjectId> {

        A aQuery(int anInt) {
          findOne(createQuery().order('-aInt', anInt))
        }

      }
      null
    '''
    assert message.contains('No such persisted field: aInt for class: com.shils.morphia.A')
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
