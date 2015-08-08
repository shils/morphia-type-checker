package com.shils

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.Embedded
import org.mongodb.morphia.annotations.Entity
import org.mongodb.morphia.annotations.Id;

class MorphiaTypeCheckingExtensionTest extends GroovyTestCase {

  void testIncorrectFieldQueryShouldFail(){
    def message = shouldFail '''
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.A

      @CompileStatic(extensions = ['com.shils.MorphiaTypeCheckingExtension'])
      class ADao extends BasicDAO<A, ObjectId> {

        A aQuery(int anInt) {
          findOne(createQuery().field('aInt').equal(anInt))
        }

      }
      null
    '''
    assert message.contains('No such persisted field: aInt for class: com.shils.A')
  }

  void testEmbeddedFieldQueryShouldNotFail(){
    assertScript '''
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic
      import com.shils.A

      @CompileStatic(extensions = ['com.shils.MorphiaTypeCheckingExtension'])
      class ADao extends BasicDAO<A, ObjectId> {

        A anEmbeddedQuery(String embeddedAString) {
          findOne(createQuery().field('embedded.aString').equal(embeddedAString))
        }

      }
      null
    '''
  }
}

@Entity
class A {
  @Id
  ObjectId id
  int anInt
  String aString
  B embedded
}

@Embedded
class B {
  String aString
}
