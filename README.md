# Compile time Morphia validation

[![Build Status](https://travis-ci.org/shils/morphia-type-checker.svg?branch=master)](https://travis-ci.org/shils/morphia-type-checker)
[![codecov.io](https://img.shields.io/codecov/c/github/shils/morphia-type-checker/master.svg)](http://codecov.io/github/shils/morphia-type-checker?branch=master)

### A quick example

Within the [Morphia](https://github.com/mongodb/morphia) library, there are many places where `String` objects are used to refer to a field of a class. These usages aren't normally validated by the compiler; for example, the following code compiles without any indication that the `User` class doesn't have a `userd` field:

```groovy
import org.mongodb.morphia.annotations.*
import groovy.transform.CompileStatic
import org.bson.types.ObjectId

@Entity
@Indexes(@Index(fields = @Field('userd')))
@CompileStatic
class User {
  @Id
  ObjectId id
  int userId
}
```

The error would be discovered at runtime, hopefully before it reached a production environment. If only the compiler knew that `'userd'` was *supposed* to be the name of a field of the `User` class...

Fortunately, using Groovy [type checking extensions](http://docs.groovy-lang.org/latest/html/documentation/#_type_checking_extensions), we can make the compiler aware of what we already know. If we apply the `EntityTypeCheckingExtension` to our `User` class,

```groovy
@Entity
@Indexes(@Index(fields = @Field('userd')))
@CompileStatic(extensions = 'me.shils.morphia.EntityTypeCheckingExtension')
class User {
  @Id
  ObjectId id
  int userId
}
```

our code will fail compilation with the following error:

```[Static type checking] - No such persisted field: userd for class: User```




## Usage

The easiest way to validate your Morphia Entities and DAOs at compile time is by using a [config script](http://docs.groovy-lang.org/latest/html/documentation/#_config_script_flag). Just add an instance of `MorphiaTypeCheckingCustomizer` to the bound `configuration` variable and the appropriate type checking extensions will be applied to your Entities (classes annotated with `@Entity`) and DAOs (classes that implement the `DAO` interface):

```groovy
import me.shils.morphia.MorphiaTypeCheckingCustomizer

configuration.addCompilationCustomizers(new MorphiaTypeCheckingCustomizer())
```

Alternatively, you can add the the appropriate extensions to your statically type checked classes yourself - `EntityTypeCheckingExtension` for Entities, and `DAOTypeCheckingExtension` for DAOs.
