# Compile time Morphia validation

[![Build Status](https://travis-ci.org/shils/morphia-type-checker.svg?branch=master)](https://travis-ci.org/shils/morphia-type-checker)
[![codecov.io](https://img.shields.io/codecov/c/github/shils/morphia-type-checker/master.svg)](http://codecov.io/github/shils/morphia-type-checker?branch=master)

## Latest Version

The latest release version is **0.1.0** and is available on Maven Central at the following coordinates:

`groupId: me.shils, artifactId: morphia-type-checker, version: 0.1.0`

## A quick example

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

## Pre-Usage

To use Morphia Type Checker, you'll first need it on your compiler classpath. You can add it as a dependency with [Maven](#maven) or [Gradle](#gradle), or build the jar from [source](#building-from-source) if you prefer.

You must also be using (at the minimum):

**Java**: 7

**Groovy**: 2.4.0

**Morphia**: 0.107

# Usage

The easiest way to validate your Morphia Entities (classes annotated with `@Entity`) and DAOs (classes that implement the `DAO` interface) at compile time is by using a [config script](http://docs.groovy-lang.org/latest/html/documentation/#_config_script_flag). Just add an instance of `MorphiaTypeCheckingCustomizer` to the bound `configuration` variable and the appropriate type checking extensions will be applied to your Entities and DAOs:

```groovy
import me.shils.morphia.MorphiaTypeCheckingCustomizer

configuration.addCompilationCustomizers(new MorphiaTypeCheckingCustomizer())
```

Alternatively, you can add the appropriate extensions to your statically type checked classes yourself - `EntityTypeCheckingExtension` for Entities, and `DAOTypeCheckingExtension` for DAOs.

# Extensions

The following type checking extensions are used to validate the usage of strings referencing fields in different contexts.

### DAOTypeCheckingExtension

When applied to DAOs, this extension validates:

* arguments to `Query` methods `criteria`, `field`, `filter`, and `order`
* arguments to `UpdateOperations` methods `add`, `addAll`, `dec`, `inc`, `max`, `min`, `removeAll`, `removeFirst`, `removeLast`, `set`, `setOnInsert`, and `unset`

**It is assumed that queries and updates within the DAO target the DAO's entity class.**

### EntityTypeCheckingExtension

When applied to Entities, this extension validates:

* `value` members of `@Index` and `@Field`

# Dependency Snippets

#### Maven

```
<dependency>
  <groupId>me.shils</groupId>
  <artifactId>morphia-type-checker</artifactId>
  <version>0.1.0</version>
</dependency>
```

#### Gradle

```
dependencies {
  compile 'me.shils:morphia-type-checker:0.1.0'
}
```

# Building from source

To build from source, just [clone](https://github.com/shils/morphia-type-checker.git) this repo and run `./gradlew build` from the root directory of the project.