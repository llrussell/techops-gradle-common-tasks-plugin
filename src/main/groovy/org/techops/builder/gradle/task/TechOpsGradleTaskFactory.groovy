package org.techops.builder.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class TechOpsGradleTaskFactory extends DefaultTask {
    def destination = "${project.buildDir}/version.txt"
    def verifiedFileLocation = "${project.buildDir}/verified.txt"

    File getVerifiedFileLocation() {
        project.file(verifiedFileLocation)
    }

    File getDestination() {
        project.file(destination)
    }

    @TaskAction
    def generateVersionFile(){
        if (project.version == "unspecified") {
            return
        }

        def file = getDestination()
        file.parentFile.mkdirs()
        file.write "$project.version"
        println("Version.txt was created in the build directory; version number is $project.version")
    }

    @TaskAction
    def verifyNexusArtifact() {
        if (project.version == "unspecified") {
            return
        }

        String artifact = "${project.name}-${project.version}"
        println("searching for https://nexus-gss.uscis.dhs.gov/nexus/content/repositories/PCIM-maven-internal/gov/dhs/uscis/pcim/pcs/${project.name}/${project.version}/${artifact}.jar")

        String statusCode = ("curl -sw '%{http_code}' https://nexus-gss.uscis.dhs.gov/nexus/content/repositories/PCIM-maven-internal/gov/dhs/uscis/pcim/pcs/${project.name}/${project.version}/${artifact}.jar -O -v").execute().text
        println ("Search Http status code [ $statusCode ]")

        if (statusCode.equalsIgnoreCase("'200'") || statusCode.equalsIgnoreCase("'000'")){
            def file = getVerifiedFileLocation()
            file.parentFile.mkdirs()
            file.write "verified"
            println("Artifact ${artifact}.jar was found in nexus")
        } else {
            println("Artifact ${artifact}.jar not found in nexus")
        }

        "rm -rf ${artifact}.jar".execute() // Removes unnecessary binary from being produced into
    }

}