package me.shils.morphia.annotations

import groovy.transform.Undefined

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.PARAMETER])
@interface FieldName {
  Class value() default Undefined.CLASS

  int parameterIndex() default - 1

  int genericTypeIndex() default - 1
}
