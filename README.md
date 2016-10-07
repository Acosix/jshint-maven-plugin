# JSHint Maven Plugin

This plugin provides the ability to run [JSHint](http://www.jshint.com) validations on your JavaScript source files.

THe need for this custom plugin is based on the fact that any existing plugin(s) has/have fallen out of grace with their original developer(s). I.e. [cjdev/jshint-mojo](https://github.com/cjdev/jshint-mojo) (linked from the JSHint homepage itself) has last been updated on Mar 20th, 2015 and to date has 16 issues and 12 PRs pending.

## Goals
* `jshint-maven-plugin:jshint`: runs JSHint on your JavaScript files based on the global plugin configuration

## Configuration Options

| Option          | Default Value                 | Explanation  |
| --------------- | :---------------------------: | ------------ |
| sourceDirectory | `${project.baseDir}/src/main` | The directory in which JavaScript sources will be processed |
| jsHintDefaultConfigFile | `jshint.config.json`   | The JSON file to specify default options to JSHint - the mojo will also pick up configuration files from .jshintrc on script file paths |
| incldues        | `*.js, **/*.js`             | The inclusion pattern to match script files that should be processed |
| excldues        |                               | The exclusion pattern to match script files that should not be processed |
| failOnError     | `true`                       | The flag specifying if the whole project build should fail when JSHint errors are found |
| preferRhino     | `false`                       | The flag specifying that Rhino should be the preferred execution engine for JSHint even if Nashorn is available on the current platform. If this is set to `false` the mojo will automatically use Nashorn when it is available in the version of the JDK used to run the build. |
| ignoreJSHintIgnoreFiles | `false`                       | The flag specifying that any .jshintignore files found on paths within the source directory should be ignored and their contents not be added to the excludes list |
| ignoreJSHintConfigFiles | `false`                       | The flag specifying that any .jshintrc files found on paths within the source directory should be ignored and the configuration therein not supersede the default configuration |
| jshintVersion   | 2.9.3                         | The version of the embedded JSHint script to use |
| jshintScript    |                               | The path to a custom JSHint script to be used - this supports resolution of JSHint scripts in the projects directory structure as well as the classpath of the plugin including any dependencies (for reusable script packaging) |


## Example Configurations

```xml
<plugin>
     <groupId>de.acosix.maven</groupId>
     <artifactId>jshint-maven-plugin</artifactId>
     <version>0.0.1-SNAPSHOT</version>
     <configuration>
         <jsHintDefaultConfigFile>src/main/resources/jshint.conf.json</jsHintDefaultConfigFile>
         <sourceDirectory>src/main/web</sourceDirectory>
         <excludes>
              <exclude>**/*-min.js</exclude>
         </excludes>
         <failOnError>false</failOnError>
     </configuration>
</plugin>
```