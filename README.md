# TechOps Gradle plugin
[![Build Status]()

A helper plugin for the gradle build tool.

## Rational

Engineers have developed multiple applications, libraries and services which
relies heavily on the gradle build tool.  Due to the growing number of projects
being developed using this build tool, I have noticed a mass expansion of unnecessary 
duplicated build script logic across all our projects. I needed a way to properly
manage this growing concern. All of our projects are built as Java applications
which requires commonly used plugins. Instead of defining these in each individual
application build script, this plugin is used to abstract away the common occurring plugins
and their respective configuration.

### Encapsulated plugins
``` groovy
plugins {
    id "java"
    id "groovy"
    id "maven_publish"
    id "idea"
    id "eclipse"
    id "jsonschema2pojo"
    id "asciidoctor"
    id "org.unbroken-dome.gitversion"
    id "gradle-credentials-plugin"
    id "checkstyle"
    id "pmd"
    id "spotbugs-gradle-plugin"
    id "org.sonarqube"
    id "jacoco"
}
```

## Quickstart

### Applying the Plugin

Add the following at the beginning of your Gradle build script:

``` groovy
plugins {
  id "techops-gradle-plugin" version "1.0.82"
}
```

Applying the plugin installs a `techopsGradlePlugin` extension in the DSL, as well as some commonly use tasks.

#### Git versioning
This plugin make use of the gitversion plugin developed by org.unbroken-dome.
Its already configured with the necessary rules to generate the version that's shared among the projects.
Version is by default calculated using the total checkin to master. There are
two attributes to the extension that allows the teams a bit of flexibility with
managing the version: major.minor.patch values.

``` groovy
techopsGradlePlugin {
    majorOffset = 1
    patchOffset = 0
}
```

The above snippet is configured to allow the major offset to be increased by 1.
The rules used is as follows

```groovy
gitVersion.rules {
    int offsetCommitCount = extension.commitCount + extension.patchOffset + (extension.majorOffset * 1000)
    println("total Commit + Offsets to process: $offsetCommitCount")
    int patch = (offsetCommitCount) % 100
    int minor = (int) (offsetCommitCount - (extension.majorOffset * 1000)) / 100
    int major = (int) (offsetCommitCount / 1000)
 }

```

##### Example 1: Increasing the major value

Total commits to master is 334.
  extension.commitCount = 334
  majorOffset = 1
  patchOffset = 0
  
``` groovy
int offsetCommitCount = extension.commitCount + extension.patchOffset + (extension.majorOffset * 1000)
```
offsetCommiCount = 334 + 0 + (1 * 1000) = 1334

```groovy
 int patch = (offsetCommitCount) % 100
 int minor = (int) (offsetCommitCount - (extension.majorOffset * 1000)) / 100
 int major = (int) (offsetCommitCount / 1000)
```

```
    patch = 1334 % 100 = 34
    minor = (1334 - (1 * 1000)) / 100 = 3
    major = 1334 / 1000 = 1
```

version would them be 1.3.34

##### Example 2: increasing the minor value

Using the same commit count from before of 334. Say you want to now have a 
version number of 1.4.0, in essence raising the minor value.
You would want to set the patch offset to bring the some of the total commit
to the nearest 100. The nearest 100th to 334 is 400, so (400-334) = 66
you will want to set the patchOffset to 66.

``` groovy
techopsGradlePlugin {
    majorOffset = 1
    patchOffset = 66
}
```
Following the rule calculations:

```
    patch = 1400 % 100 = 0
    minor = (1400 - (1 * 1000)) / 100 = 4
    major = 1400 / 1000 = 1
```
version would them be 1.4.0

### Activating specific plugins
There will be certain plugins configured which you may not neccessarly need as part
of your buildscript. I have made thse plugins optionally visible. Currently
as off this version only one such plugin the JsonSchemePlugin is configured like this.
By default it is turn off but in the event that you need this plugin for your project, its 
easlity activated by setting the extention value needs_jsonschema2pojo.

``` groovy
techopsGradlePlugin {
    majorOffset = 1
    patchOffset = 0
    needs_Docker = true
}
```


## Future Enhancements

Will be adding extension to allow certain plugins to be configured based
on the language or other specific plugins.

``` groovy
techopsGradlePlugin {
//language options
    is_GroovyProject
    is_JavaProject
    is_KotlinProject
    is_PythonProject

//Specific plugin needed
    needs_springboot 
    needs_spring      
}
```
