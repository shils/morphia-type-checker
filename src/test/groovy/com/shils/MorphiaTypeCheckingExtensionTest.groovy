package com.shils


class MorphiaTypeCheckingExtensionTest extends GroovyTestCase {

  void testQueryField(){
    shouldFail '''
      import org.mongodb.morphia.annotations.Entity
      import org.mongodb.morphia.dao.BasicDAO
      import org.bson.types.ObjectId
      import groovy.transform.CompileStatic

      @Entity
      class A {
        int anInt
        String aString
        List<Date> aList
      }

      @CompileStatic(extensions = ['com.shils.MorphiaTypeCheckingExtension'])
      class ADao extends BasicDAO<A, ObjectId> {

        A aQuery(A a) {
          findOne(createQuery().field('anIn').equal(a.anInt))
        }

      }
      null
    '''

  }



}
