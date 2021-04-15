## 1.4.2 (2019-06-09 03:45:24)
### No issue

**Noteworthy Changes:**

* -------------------------------------
* Add pipeline step chaining: would now have the ability to change multiple pcs-pipeline steps together.
* example:
* before*
* techOpsPipelineStep.checkOutFrom(&#39;techOps-gradle-plugin.git&#39;, env.BRANCH_NAME)
* techOpsPipelineStep.publishToNexus(true)
* techOpsPipelineStep.generateChangelog()
* after*
* techOpsPipelineStep.checkOutFrom(&#39;pcim-techOps-gradle-plugin.git&#39;, env.BRANCH_NAME)
* .publishToNexus(true)
* .generateChangelog()
* Possibly code breaking changes:
* -------------------------------------
* - Removed the CreateTag gradle task, since its no longer neccessary. That action was moved over to Jenkins to handle. 
    Instead there now is a task to create a version.txt file to temparayly hold the version number during the build process.
* - Modify pipeline step to publish to nexus. More of thE controls are handle directly by Jenkins and the Nexus platform plugin.

llrussel *2019-06-09 03:45:24*

**testing chaining part 3**

* updates to use the 1.3.77-SNAPSHOT
* regroup, not switching back to using default branch from pipeline libraries. This allows separate jenkins to manage which default branch to use.
* Changelog updated by Jenkins
* minor changes
* just for test
* testing chaining part 2
* testing chaining part 3
* test git tagging stage
* test git nexus publish stage
* testing a new version of the plugin to allow for versionfile creation
* updates to use the 1.3.77-SNAPSHOT
* Changelog updated by Jenkins
* minor changes
* just for test
* testing chaining part 2

llrussel *2019-06-09 03:13:18*

**test git nexus publish stage**

* testing a new version of the plugin to allow for versionfile creation
* updates to use the 1.3.77-SNAPSHOT
* Changelog updated by Jenkins
* minor changes
* just for test
* testing chaining part 2

**remove post stage step**

llrussel *2019-05-20 17:40:05*


**Major checkins of pcs first gradle plugin to streaming our build scripts.**

* Also add and configure the following plugins
* - gitVersion
* - maven-publish
* - checkstyle
* - pmd
* - java
* - groovy

llrussel *2019-04-24 20:00:46*