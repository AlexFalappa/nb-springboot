# NB-SpringBoot

NetBeans IDE plugin supporting programming with [Spring Boot](http://projects.spring.io/spring-boot).


## Requirements and Installation

This plugin requires **NetBeans 8.2** or above running on **JDK 8** or above.

To perform a manual install download the desired `nbm` package from the [releases page](https://github.com/AlexFalappa/nb-springboot/releases) , go to *Tools > Plugins > Downloaded* and click the *Add Plugins...* button. Choose the downloaded `nbm` package file then click *Install*.

The plugin may be available in the *Plugin Portal Update Center* thus it can be downloaded and installed trough *Tools > Plugins > Available Plugins*. Please note that new releases become available on the *Update Center* after they are verified by the folks at the *NetBeans Plugin Portal*. Note that current NetBeans Plugin Portal (http://plugins.netbeans.org) is undergoing migration at Apache Foundation. A new plugin portal is being set-up (currently at http://netbeans-vm.apache.org/pluginportal but subject to change) and may become the new official source of plugins.

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
