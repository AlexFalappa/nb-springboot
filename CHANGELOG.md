# Changelog

-  **2.0.1** (July 2018): Polishing
    -  Use HTTPS protocol when contacting Spring Initializr service
    -  New Java files code template for command line runner beans
    -  Small behavior change: the Spring Boot Restart action in the toolbar is enabled only if a project node is selected in the Projects view or its _Dependencies_, _Runtime Dependencies_ or _Test Dependencies_ inner nodes
    -  [List of closed issues](https://github.com/AlexFalappa/nb-springboot/milestone/15?closed=1)

-  **2.0** (Mar 2018): Reactive Goodies
    -  Support for projects based on Spring Boot 2.x
    -  Refined the *Spring Initializr* project wizard:
        - presents frequently used starter dependencies first
        - linked editing of some base project properties (i.e. artifactId linked to name and package)
    -  New File templates for:
        - actuator endpoints 
        - reactive `@RestController` annotated classes
        - reactive repository interfaces
        - reactive handler classes
        - classes implementing `InfoContributor`
        - classes implementing `HealthIndicator`
    -  File templates for `@Controller` and `@RestController` annotated classes allow to generate error handling methods
    -  New code templates:
        - in Java files for web request mapping annotations and webflux router functions beans
        - in configuration properties files for commonly used sets of properties 
    -  Changes in maven `pom.xml` management in the *Spring Initializr* project wizard:
        -  Do not make `spring-boot-devtools` dependency optional by default (use the [`excludeDevtools` option of the spring boot maven plugin](https://docs.spring.io/spring-boot/docs/current/maven-plugin/repackage-mojo.html) to control inclusion of devtools in repackaged archive)
        -  Do not set `fork` to true to `spring-boot-maven-plugin` configuration section (forking a jvm is automatic when needed)
    -  Show deprecated configuration properties of level error by default (only effective if no previous plugin preferences found)
    -  [List of closed issues](https://github.com/AlexFalappa/nb-springboot/milestone/13?closed=1)

-  **1.6.1** (Oct 2017): Hints and quick fixes
    -  Java editor hints:
        -  Warning and fix on use of `@ConfigurationProperties` annotation without `spring-boot-configuration-processor` dependency
        -  Warning and fixes on import of annotations/interfaces of Spring MVC/Spring Data without relevant dependencies
    -  Quick fixes:
        -  Removal of deprecated,unknown or duplicate properties
        -  Substitution of deprecated properties with replacement (if present in metadata)
    -  Error highlighting in configuration properties editor:
        -  Added highlighting for deprecated properties according to their level in metadata
        -  Relaxed configuration properties name variants are recognized
    -  Upgrade to Spring Boot 1.5.8
    -  [List of closed issues](https://github.com/AlexFalappa/nb-springboot/milestone/12?closed=1)

-  **1.6** (Jul 2017): Configuration properties highlighting
    -  Error highlighting in configuration properties editor for:
        -  Syntax errors
        -  Duplicate properties
        -  Data type mismatches *(see documentation for limitations)*
        -  Unknown properties
    -  Upgrade to Spring Boot 1.5.4
    -  Manage deprecation error level in configuration properties metadata
    -  New plugin options for customizing error higlighting severity
    -  New plugin options for configuration properties lists used in completion and override on launch:
        -  Include deprecated properties with level error
        -  Show all deprecated properties last
    -  Style of configuration properties items in lists is more uniform
    -  The `application.properties` file wizard allows to choose between *main* and *test* resource folders
    -  [List of closed issues](https://github.com/AlexFalappa/nb-springboot/milestone/11?closed=1)

-  **1.5.1** (May 2017): Another round of fixes
    -  Upgrade to Spring Boot 1.5.3
    -  Projects wizard now open `pom.xml` instead of main class after generation
    -  Internal reworkings to improve plugin initialization and reaction to project build and configuration properties changes
    -  Focus filter textfield in *Add Spring Boot Dependencies* dialog
    -  [List of closed issues](https://github.com/AlexFalappa/nb-springboot/milestone/10?closed=1)

-  **1.5** (Apr 2017): Dependencies management and configuration properties editor
    -  Configuration properties files:
        -  Custom icon
        -  Specific editor with extended syntax highlighting (dot separated keys and array notation)
    -  The *Spring Initializr* project wizard now supports documentation links recently added to the *Spring Initializr* web service
    -  The old code generators for Maven `pom.xml` files have been superseded by a new code generator for adding a set of Spring Boot dependencies exploiting the *Spring Initializr* service metadata
    -  New code generator for Maven `pom.xml` to add a basic Spring Boot project setup to generic Maven projects
    -  Project wizards open the main class and trigger async download of dependencies after creation
    -  Metadata downloaded from the *Spring Initializr* web service are now cached
    -  Upgrade to Spring Boot 1.5.2
    -  [List of closed issues](https://github.com/AlexFalappa/nb-springboot/milestone/8?closed=1)

-  **1.4** (January 2017): Better application launch
    -  Improved project properties panel:
        -  Override of Spring Boot configuration properties at application launch
        -  Checkboxes for enabling debug mode and toggling color output
        -  Specify Java VM options and add launch optimizations
    -  Default launch options for newly created projects in global plugin settings
    -  Updated project content for Basic Spring Boot project wizard
    -  [List of closed issues](https://github.com/AlexFalappa/nb-springboot/milestone/6?closed=1)

-  **1.3.1** (November 2016): General fixes
    -  File templates ignored a per project custom license
    -  Reload action renamed to restart and shorcut changed to avoid conflict with new *Paste as Lines* command in editor multicaret
    -  Visual improvements to request mappings navigator panel
    -  Properly handle `Ctrl + Enter` configuration properties completion to overwrite the current property name
    -  Initializr project wizard: removed option to add Spring Boot configuration processor as now it can be chosen in the Dependencies page and checked by default the 'Run/Debug with spring boot maven plugin' option
    -  Restart action now exploits an environment variable instead of a command line argument. You may see an unexpected `--spring.devtools.restart.trigger-file` argument on old projects after upgrade
    -  [List of closed issues](https://github.com/AlexFalappa/nb-springboot/milestone/7?closed=1)

-  **1.3** (October 2016): Devtools and request mappings
    -  Upgrade to Spring Boot version 1.4.1
    -  Action to trigger devtools reload (trough toolbar button or `Ctrl + Shift + L` keyboard shortcut)
    -  Spring boot dedicated panel in project properties dialog for specifying command line arguments and enabling devtools reload trigger
    -  Moved some file templates to the default *Spring Framework* category
    -  Navigator panel showing request URL mappings of a `Controller` / `RestController` class (contribution by [Michael Simons](http://michael-simons.eu))
    -  [List of closed issues](https://github.com/AlexFalappa/nb-springboot/milestone/5?closed=1)

-  **1.2** (August 2016): File templates improvements
    -  Upgrade to Spring Boot version 1.4
    -  The file templates wizards that generate Java classes now use the standard NetBeans widgets (better name proposal, choice of source/test location and destination package)
    -  The wizards for `application.properties` files, additional configuration metadata and Spring Data repository interfaces now offer specific customization options
    -  The file templates are now shown only if the relevant dependencies are present (e.g. REST Controller Class is shown only if there is a dependency on spring-boot-starter-web)
    -  Devtools restart and reload now work when the project is run/debugged trough the spring maven plugin
    -  Fixed some UI glitches
    -  Enhanced Controller and RestController templates
    -  [List of closed issues](https://github.com/AlexFalappa/nb-springboot/milestone/4?closed=1)

-  **1.1** (June 2016): Refinements
    -  Added missing `@Component` annotation to CommandLineRunner and ApplicationRunner file templates
    -  No more NetBeans module implementation dependencies, this allows the plugin to be available from the Update Center
    -  Improvements to New project wizard Spring Boot project (from Initializr):
        -  Asynchronous querying of the web service
        -  More paramenter validation
        -  Filtering of dependencies
        -  Show required boot version in tooltips of disabled dependencies
        -  Options to remove the maven wrapper, add the spring configuration processor dependency and run/debug trough the maven spring plugin
    -  [List of closed issues](https://github.com/AlexFalappa/nb-springboot/milestone/3?closed=1)

-  **1.0** (May 2016): Initial public release
