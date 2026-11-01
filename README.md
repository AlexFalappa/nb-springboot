# NB-SpringBoot

NetBeans IDE plugin supporting programming with [Spring Boot](http://projects.spring.io/spring-boot).

![Downloads](https://img.shields.io/endpoint?url=https://openbeans.org/plugin-counter/api/4)

## Requirements and Installation

This development version builds against **Apache NetBeans 30** by default. Use **JDK 21** and **Maven 3.8 or later** to build it:

```sh
mvn clean verify
```

The files are generated as `target/nb-springboot-plugin-3.2.jar` and `target/nbm/nb-springboot-plugin-3.2.nbm`. The module identity, name, authors and Apache 2.0 license are preserved. This is a local development build, not a release published by the original author or verified by the NetBeans Plugin Portal.

### Targeted NetBeans builds

The Maven integration requires implementation dependencies on NetBeans Maven modules. Some of these modules include the IDE build identifier in their implementation version. Consequently, **one NBM cannot be claimed to work across all NetBeans releases**. Build for the IDE you intend to use:

```sh
mvn -Dnetbeans.target=22 clean verify
mvn -Dnetbeans.target=30 clean verify
```

These select `RELEASE220` and `RELEASE300`, respectively. Rebuild cleanly when switching targets. NetBeans 32 remains a future target: it requires its published APIs, a separate build and validation. Do not install the NetBeans 22 package in NetBeans 30.

The plugin's own sources target Java 17; the JDK running NetBeans must also meet that IDE's requirements. An application's Java version is separate: the Initializr wizard validates the Java minimum for the selected Spring Boot generation.

### Modernization scope

- Spring Framework 6.2, configuration metadata 3.5, Jackson 2.21 and Parboiled 1.4 replace the older embedded libraries.
- Run/debug properties use the modern Maven prefix for Spring Boot 2, 3 and 4.
- `nbactions.xml` migration changes recognized properties only in Spring Boot run actions, preserves unrelated actions and uses atomic replacement. If that operation is unsupported, the original is preserved and a warning is logged.
- The legacy Yenta installer that changed access to NetBeans internals has been removed. Maven dependencies are explicit and package verification remains strict.
- Controller templates choose `javax.servlet` for Boot 2 and `jakarta.servlet` for Boot 3/4, with compile-classpath detection for projects without an identified Boot version.
- The offline basic project uses Spring Boot 3.5.7 and Java 17; Initializr provides the versions currently offered by its service.
- Initializr cache invalidation is fixed, service errors propagate to the creation wizard and incompatible Java/Boot combinations are rejected.
- Injecting Boot setup preserves existing build plugins and refuses to overwrite an existing parent POM.
- Tests run by default. Legacy standalone lexer tests remain excluded because they require a different generated scanner; existing parser and combined lexer tests still run.

Detailed test output is saved under `target/surefire-reports/`; the console retains test summaries and failure reports. SLF4J uses the Java logging backend so plugin messages can use the IDE's logging infrastructure.

Deterministic tests use a local HTTP server. To additionally test the real Initializr metadata, dependencies and project archive:

```sh
mvn -Dinitializr.live=true clean verify
```

Interactive installation, Run/Debug, DevTools and editor testing inside each target IDE are still required before declaring that IDE version fully verified. YAML completion, configuration imports and multidocument configuration support remain future work. The upstream issue about editing NetBeans' Run panel is not yet verified as resolved.

Validation on 2026-11-01: the NetBeans 30 package built cleanly with JDK 21 and strict NBM dependency verification. All 99 tests passed, including live Initializr metadata, dependencies and project generation. All 43 declared module dependencies were compared successfully with the installed NetBeans 30 distribution. Earlier regression runs against the NetBeans 22 APIs passed on Java 17 and 21; the final NetBeans 30 package requires its own matching IDE modules.

To perform a manual install download the desired `nbm` package from the [releases page](https://github.com/AlexFalappa/nb-springboot/releases) , go to *Tools > Plugins > Downloaded* and click the *Add Plugins...* button. Choose the downloaded `nbm` package file then click *Install*.

The upstream plugin is listed in the [NetBeans Plugin Portal](https://plugins.netbeans.apache.org/catalogue/?id=4). That listing does not certify this development build.

## Features

-  *Spring Boot* Maven project wizards:
    -  Basic project
    -  Project generated trough the *Spring Initializr* service (https://start.spring.io)
-  Specific editor for configuration properties files with:
    -  extended syntax highlighting (dot separated keys, array notation)
    -  error highlighting for: syntax errors, duplicate properties, data type mismatches and unknown properties
    -  quick fixes for: removing deprecated, unknown and duplicate properties or substituting deprecated properties with their replacement
    -  completion and documentation of configuration properties names and values (with support of [hint value providers](https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-configuration-metadata.html#value-providers) defined in configuration metadata)
-  Java editor error highlighting and fixes for missing starter dependencies:
    - @ConfigurationProperties annotation without `spring-boot-configuration-processor` dependency
    - Imports of Spring MVC annotations and Spring Data interfaces without relevant starter dependencies (e.g. `JpaRepository` without `spring-boot-starter-data-jpa` dependency)
-  Code templates:
    - Java templates for web request mapping annotations, WebFlux router functions and `CommandLineRunner` beans
    - Configuration properties templates for commonly used sets of properties 
-  *Spring Boot* file templates:
    -  `CommandlineRunner` classes
    -  `ApplicationRunner` classes
    -  `application.properties` files
    -  `additional-spring-configuration-metadata.json` files
    -  `@ConfigurationProperties` annotated classes
-  *Spring Boot Actuator* file templates:
    -  Actuator endpoints 
    -  Classes implementing `InfoContributor`
    -  Classes implementing `HealthIndicator`
-  Additional *Spring Framework* file templates:
    -  `@Component` annotated classes
    -  `@Configuration` annotated classes
    -  `@Service` annotated classes
    -  `@Controller` annotated classes (Spring MVC)
    -  `@RestController` annotated classes (Spring MVC)
    -  Reactive `@RestController` annotated classes (Spring WebFlux)
    -  Reactive handler classes (Spring WebFlux)
    -  Interfaces extending `Repository` both imperative and reactive (Spring Data)
-  Additional code generators in `pom.xml` files:
    -  Add Spring Boot dependencies (dependency metadata is taken from the *Spring Initializr* web service)
    -  Add basic Spring Boot setup
-  Toolbar button to trigger *Spring Boot* devtools reload
-  Custom *Spring Boot* project properties page to:
    -  Specify command line run arguments and launch VM options
    -  Enable/disable manual devtools reload trigger
    -  Toggle debug mode and color output
    -  Assisted override of configuration properties at launch
-  Additional navigator panel to show request URL mappings of a `Controller` / `RestController` class

## Issues and Documentation

Bug tracking: [GitHub Issues](https://github.com/AlexFalappa/nb-springboot/issues)

Getting Started: [Quick Tour](https://github.com/AlexFalappa/nb-springboot/wiki/Quick-Tour)

Reference: [GitHub Wiki](https://github.com/AlexFalappa/nb-springboot/wiki)

## License

The plugin and its source code are licensed under [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0).


## Acknowledgements

Completion of Spring Boot configuration properties feature is based on [Keevosh plugin](https://github.com/keevosh/nb-springboot-configuration-support).

Templates and code generators were inspired by those found on [Spring Boot Tools 4 NetBeans](https://github.com/GeertjanWielenga/SpringBootTools4NetBeans).

Requestmappings navigator panel feature contributed by [Michael Simons](https://github.com/michael-simons).

Badge on projects icon feature initially contributed by [Hector Espert](https://github.com/blackleg).

## Changelog

See [the separate file](CHANGELOG.md)
