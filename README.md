# Detekt Plugin: Verify Implementation

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/dk.cachet.detekt.extensions/detekt-verify-implementation/badge.svg)](https://mvnrepository.com/artifact/dk.cachet.detekt.extensions/detekt-verify-implementation)

This is a plugin for [detekt](https://detekt.github.io/detekt/), a static code analyzer for Kotlin,
which enables verifying whether concrete classes are implemented as specified **according to
annotations applied to base types**.

The following detekt rules are included:

-  `DataClass`: requires extending classes to be [data classes](https://kotlinlang.org/docs/reference/data-classes.html) or [object declarations](https://kotlinlang.org/docs/object-declarations.html#object-declarations).
   This guarantees a predictable default implementation for `equals` and `hashcode` implementations, i.e., value equality instead of referential equality.
-  `Immutable`: requires classes or extending classes to be immutable;
they may not contain mutable properties (var) or properties of mutable types (types with var properties).

Due to the nature of this plugin, [a specific flavor of semantic versioning is used which makes more sense for linters](https://stylelint.io/about/semantic-versioning). Any minor update may report more errors than the previous release.


## Enabling the Plugin in Gradle

Load the plugin through the [detekt Gradle configuration block](https://detekt.github.io/detekt/extensions.html#let-detekt-know-about-your-extensions):

_Groovy DSL_

```
detekt {
    dependencies {
        detektPlugins "dk.cachet.detekt.extensions:detekt-verify-implementation:1.1.0"
    }
}
```

Since this plugin uses type and symbol solving, [a custom Gradle detekt task needs to be used](https://github.com/detekt/detekt/issues/2259) which sets `classpath`. In addition, `jvmTarget` needs to be set to 1.8.
An example on how to do so:

_Groovy DSL_

```
task detektPasses(type: io.gitlab.arturbosch.detekt.Detekt) {
    source = fileTree("$rootDir")
    {
        include('**/src/**')
        exclude('**/node_modules/**')
    }
    config.from("$rootDir/detekt.yml")
    classpath.setFrom(project.configurations.getByName("detekt"))
}
tasks.detekt.jvmTarget = "1.8"
```

## Configuration

By default, the rules are not activated in the [detekt configuration file](https://detekt.github.io/detekt/configurations.html).
To enable a rule, set it to `active` and specify the fully qualified name of the `annotationClass` which determines which concrete classes to verify. For example:

```
verify-implementation:
  DataClass:
    active: true
    annotationClass: "dk.cachet.carp.common.ImplementAsDataClass"
  Immutable:
    active: true
    annotationClass: "dk.cachet.carp.common.Immutable"
```

In case the annotation class cannot be found and the rule is active an `IllegalStateException` will be thrown.

Additional rule-specific configuration options are described next.

### Immutable

- `assumeImmutable` can list fully qualified class names which won't be verified and are assumed to be immutable.
This may be useful when the plugin fails to analyze classes which you know to be immutable. Example configuration:

```
verify-implementation:
  Immutable:
    active: true
    annotationClass: "dk.cachet.carp.common.Immutable"
    includes: ['**/domain/**']
    assumeImmutable: [
      'dk.cachet.carp.common.DateTime',
      'Json'
    ]
```
