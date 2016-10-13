# JSHint Maven Plugin

This plugin provides the ability to run [JSHint](http://www.jshint.com) validations on your JavaScript source files.

THe need for this custom plugin is based on the fact that any existing plugin(s) has/have fallen out of grace with their original developer(s). I.e. [cjdev/jshint-mojo](https://github.com/cjdev/jshint-mojo) (linked from the JSHint homepage itself) has last been updated on Mar 20th, 2015 and to date has 16 issues and 12 PRs pending.

## Goals
* `jshint:jshint`: runs JSHint on your JavaScript files based on the global plugin configuration

## Configuration Options

| Option          | Default Value                 | Explanation  |
| --------------- | :---------------------------: | ------------ |
| sourceDirectory | `${project.baseDir}/src/main` | The directory in which JavaScript sources will be processed |
| jsHintDefaultConfigFile | `.jshintrc`   | The JSON file to specify default options to JSHint - the mojo will also pick up configuration files from .jshintrc on script file paths |
| includes        | `*.js, **/*.js`             | The inclusion pattern to match script files that should be processed |
| excludes        |                               | The exclusion pattern to match script files that should not be processed |
| failOnError     | `true`                       | The flag specifying if the whole project build should fail when JSHint errors are found |
| preferRhino     | `false`                       | The flag specifying that Rhino should be the preferred execution engine for JSHint even if Nashorn is available on the current platform. If this is set to `false` the mojo will automatically use Nashorn when it is available in the version of the JDK used to run the build. |
| ignoreJSHintIgnoreFiles | `false`                       | The flag specifying that any .jshintignore files found on paths within the source directory should be ignored and their contents not be added to the excludes list |
| ignoreJSHintConfigFiles | `false`                       | The flag specifying that any .jshintrc files found on paths within the source directory should be ignored and the configuration therein not supersede the default configuration |
| jshintVersion   | 2.9.3                         | The version of the embedded JSHint script to use |
| jshintScript    |                               | The path to a custom JSHint script to be used - this supports resolution of JSHint scripts in the projects directory structure as well as the classpath of the plugin including any dependencies (for reusable script packaging). Please note that any custom jshintScript must support the API `JSHINT( source, options, predef )` and be [adapted to work with current Rhino / Nashorn versions](https://github.com/fabioz/jshint/commit/edb0ecf79118c65552f8de8a0af6496704f6f52b) (even though the linked commit only refers to Nashorn it also applies to Rhino as was discovered in developing this plugin) |
| checkstyleReportFile |                          | The path relative to the projects build folder where a checkstyle-like report file about issues found by the plugin should be written (report file will only be written if this is set |
| skip            | `false`                      | The flag specifying the execution of this plugin should be skipped |


## Example Configurations

```xml
<plugin>
     <groupId>de.acosix.maven</groupId>
     <artifactId>jshint-maven-plugin</artifactId>
     <version>1.0.0</version>
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