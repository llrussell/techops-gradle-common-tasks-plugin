package org.techops.builder.gradle.task

import org.gradle.api.tasks.TaskAction
import se.bjurr.gitchangelog.plugin.gradle.GitChangelogTask

class TechOpsChangelogTask extends GitChangelogTask {
    def destination = "./CHANGELOG.md"
    File changeLogfile = project.file(destination)

    @TaskAction
    void generateChangeLog() {

        changeLogfile.parentFile.mkdirs()

        println("---------- Generating ChangeLog -------------")
        file = new File("./build/CHANGELOG.md")
        templateContent = """
{{#tags}}
## {{name}} ({{tagTime}})
{{#issues}}
{{#hasIssue}}
{{#hasLink}}
### {{name}} [{{issue}}]({{link}}) {{title}} {{#hasIssueType}} *{{issueType}}* {{/hasIssueType}} {{#hasLabels}} {{#labels}} *{{.}}* {{/labels}} {{/hasLabels}}
{{/hasLink}}
{{^hasLink}}
### {{name}} {{issue}} {{title}} {{#hasIssueType}} *{{issueType}}* {{/hasIssueType}} {{#hasLabels}} {{#labels}} *{{.}}* {{/labels}} {{/hasLabels}}
{{/hasLink}}
{{/hasIssue}}
{{^hasIssue}}
### {{name}}
{{/hasIssue}}

{{#commits}}
**{{{messageTitle}}}**

{{#messageBodyItems}}
* {{.}}
{{/messageBodyItems}}

[{{hash}}](https://git.uscis.dhs.gov/{{ownerName}}/{{repoName}}/commit/{{hash}}) {{authorName}} *{{commitTime}}*

{{/commits}}
{{/issues}}
{{/tags}}
         """

        gitChangelogPluginTasks()
        changeLogfile.write(file.text)

        println "------- commit the updated changelog ---------"
        println ("pre-add status::"+("git status").execute().text.trim())
        println ("add status::"+("git add CHANGELOG.md").execute().text.trim())
        println ("pre-commit status::"+("git status").execute().text.trim())

    }

}
