package org.techops.builder.gradle.plugin

import com.github.spotbugs.SpotBugsPlugin
import com.palantir.gradle.docker.DockerComposePlugin
import com.palantir.gradle.docker.DockerRunPlugin
import com.palantir.gradle.docker.PalantirDockerPlugin
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.api.plugins.quality.PmdPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.sonarqube.gradle.SonarQubePlugin
import org.techops.builder.gradle.extension.TechOpsGradlePluginExtension
import org.unbrokendome.gradle.plugins.gitversion.GitVersionPlugin
import se.bjurr.gitchangelog.plugin.gradle.GitChangelogGradlePlugin

/**
 * Plugin's main class to register all common gradle tasks used for our application. It will also apply and configure
 * gradle plugins commonly used across our application.
 *
 * <ol>Currently applied plugins:
 * <li>id: org.unbroken-dome.gitversion</li>
 * </ol>
 *
 *
 * @author linton russell (linton.l.russell@uscis.dhs.gov)
 * @since 04/24/2019
 * **/

class TechOpsGradlePlugin implements Plugin<Project> {
    public static final String GRADLE_EXTENSION_NAME = "techOpsGradlePlugin"
    private final String TECHOPS_JENKINS_SHARED_LIB_VERSION = "1.3.12"

    @Override
    void apply(Project project) {
        def extension = project.extensions.create(GRADLE_EXTENSION_NAME, TechOpsGradlePluginExtension)
        project.group = "org.techops.build"

        project.plugins.apply(IdeaPlugin)
        project.plugins.apply(EclipsePlugin)
        project.plugins.apply(GitChangelogGradlePlugin)

        if(extension.is_JavaProject) {
            project.plugins.apply(JavaPlugin)
        }
        if (extension.is_GroovyProject) {
            project.plugins.apply(GroovyPlugin)
        }
        if(extension.needs_Docker) {
            project.plugins.apply(DockerComposePlugin)
            project.plugins.apply(DockerRunPlugin)
        }

        project.afterEvaluate {
            //Apply and configure external gradle plugin
            /* id: org.unbroken-dome.gitversion */ applyAndConfigureGitVersionPlugin(project, extension ,true)
            /* id: maven-publish                */ applyAndConfigureMavenPublishPlugin(project,extension,true)
            /* id: org.gradle.quality           */ applyAndConfigureQualityPlugin(project,extension,true)
            /* id: com.palantir.docker          */ applyAndConfigureDockerPlugin(project, extension)


            project.getTasks().create('generateVersionFile', TechOpsGradleTaskFactory.class).generateVersionFile()
            project.getTasks().create('verifyNexusArtifact', TechOpsGradleTaskFactory.class).verifyNexusArtifact()
            project.getTasks().create('generateChangeLog', TechOpsChangelogTask.class).setGroup("versioning")
            project.getTasks().getByName('generateVersionFile').setGroup("versioning")
            project.getTasks().getByName('verifyNexusArtifact').setGroup("publishing")

            //validate that projects have a description set.
            if (project.description == null || project.description.isEmpty()){
                throw new GradleException('ConfigurationException: Project description is required, make sure a description' +
                        'is set i.e [ description = "an example project" ]')
            }
        }

        project.configure(project) {
            repositories {
                maven { url "${NEXUS_BASE_URL}public/" }
                maven { url "${NEXUS_BASE_URL}central/" }
            }
            project.afterEvaluate {
                wrapper {
                    distributionType = Wrapper.DistributionType.ALL
                    gradleVersion = '5.4'
                }
            }
            sourceSets {
                project.afterEvaluate {
                    //Generated
                    // Need to make api files on top to avoid compileKotlin errors
                    if (extension.needs_Jsonschema2pojo) {
                        main.java.srcDirs += "${project.buildDir}/generated-sources/js2p"
                    }
                }

                //Java & Groovy
                test.java.srcDirs += 'src/test/unit/java'
                test.resources.srcDirs += 'src/test/unit/resources'
                //Separates integration tests from unit tests
                integration_test {
                    java {
                        compileClasspath += main.output + test.output
                        runtimeClasspath += main.output + test.output
                        java.srcDirs += 'src/test/integration/java'
                    }
                    resources.srcDirs += 'src/test/integration/resources'
                }
                inttest {
                    java.srcDir file('src/inttest/java')
                    resources.srcDir file('src/inttest/resources')
                }
            }

            project.afterEvaluate {
                jar {
                    from sourceSets.main.allSource
                    manifest {
                        attributes 'Implementation-Title': project.description, 'Implementation-Version': project.version
                    }
                }

                configurations {
                    testCompile.extendsFrom compile
                    inttestCompile.extendsFrom compile
                    integration_testCompile.extendsFrom compile
                }
            }
        }
    }

    /**
     * @description  internal method to apply and configure external gradle plugins.
     *
     * @param project the gradle project instance
     * @param extension  gradle plugin extension object to allow estensibility to the main plugin.
     * @param register Boolean variable to denote is the external plugin should be auto registered.
     * */

    @CompileStatic(TypeCheckingMode.SKIP)
    private void applyAndConfigureGitVersionPlugin(Project project, TechOpsGradlePluginExtension extension, boolean register) {
        configurePlugin(project, true, register, GitVersionPlugin) {
            project.afterEvaluate {

                def getMasterCommitCountString = ("git rev-list origin/master --count").execute().text.trim()
                extension.commitCount = getMasterCommitCountString.isInteger() ? getMasterCommitCountString.toInteger() : 0

                def getLocalCommitCountString = ("git rev-list HEAD --count").execute().text.trim()
                extension.localChangesCount = getLocalCommitCountString.isInteger() ?
                                (getLocalCommitCountString.toInteger() -  extension.commitCount) : 0

                project.configure(project) {
                    println("[majorOffset:$extension.majorOffset][patchOffset:$extension.patchOffset]")

                    gitVersion.rules {
                        int offsetCommitCount = extension.commitCount + extension.patchOffset + (extension.majorOffset * 1000)
                        println("total Commit + Offsets to process: $offsetCommitCount")
                        int patch = (offsetCommitCount) % 100
                        int minor = (int) (offsetCommitCount - (extension.majorOffset * 1000)) / 100
                        int major = (int) (offsetCommitCount / 1000)

                        always {
                            version.major = major
                            version.minor = minor
                            version.patch = patch
                        }
                        //all other branches except master
                        onBranch(~/^(master.+|(?!master).*)/) {
                            int localChangesCount = extension.localChangesCount
                            version.patch += localChangesCount
                            version.prereleaseTag = 'SNAPSHOT'
                        }
                    }
                    project.version = gitVersion.determineVersion()
                    println("Total commits to local branch :[$extension.localChangesCount]")
                    println("Total commits to master:[$extension.commitCount] proposed git version from plugin: $project.version")

                }
            }
        }
    }

    private void applyAndConfigureMavenPublishPlugin(Project project, TechOpsGradlePluginExtension extension, boolean register){
        configurePlugin(project, true, register, MavenPublishPlugin) {
            project.configure(project) {

                publishing {
                    project.afterEvaluate {
                        ext {
                            repositoryInternalUrl = ''
                            repositoryUsername = project.hasProperty('repoUsername') ? project.getProperty('repoUsername') : ''
                            repositoryPassword = project.hasProperty('repoPassword') ? project.getProperty('repoPassword') : ''
                        }
                        publications {
                            mavenJava(MavenPublication) {
                                from components.java

                                groupId = project.group
                                artifactId = jar.baseName
                                version = project.version
                            }
                        }
                        repositories {
                            maven {
                                url "${repositoryInternalUrl}"
                                credentials {
                                    username "${repositoryUsername}"
                                    password "${repositoryPassword}"
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private void applyAndConfigureQualityPlugin(Project project, TechOpsGradlePluginExtension extension, boolean register){
        project.afterEvaluate {
            //---------- Configuration for Checkstyle -----------
            configurePlugin(project, true, register, CheckstylePlugin) {
                project.configure(project) {
                    checkstyle {
                        checkstyleTest.enabled = false
                        ignoreFailures = true
                    }
                }
            }
            //----------- Configuration for PMD -------------
            configurePlugin(project, true, register, PmdPlugin) {
                project.configure(project) {
                    pmd {
                        pmdTest.enabled = false
                        ignoreFailures = true
                    }
                }
            }
            //----------- Configuration for SpotBugs -------------
            configurePlugin(project, true, register, SpotBugsPlugin) {
                project.configure(project) {
                    spotbugs {
                        toolVersion = '3.1.12'
                        ignoreFailures = true
                        spotbugsTest.enabled = false
                    }
                }
            }

            //----------- Configuration for Jacoco Code Coverage -------------
            configurePlugin(project, true, register, JacocoPlugin) {
                project.configure(project) {
                    jacoco {
                        reportsDir = file("$buildDir/reports/jacoco")
                    }
                    jacocoTestReport {

                        reports {
                            xml.enabled true
                            csv.enabled false
                            html.destination file("${buildDir}/reports/jacocoHtml")
                            group = "Reporting"
                            description = "Generate Jacoco coverage reports after running tests."
                            additionalSourceDirs = files(sourceSets.main.allJava.srcDirs)
                        }

                        afterEvaluate {
                            // what to exclude from coverage report
                            // UI, "noise", generated classes, platform classes, etc.
                            def excludes = [
                                    '**/R.class',
                                    '**/R$*.class',
                                    '**/*$ViewInjector*.*',
                                    '**/BuildConfig.*',
                                    '**/Manifest*.*',
                                    '**/*Test.*',
                                    '**/*IT.*',
                                    'android/**/*.*',
                                    '**/*Fragment.*',
                                    '**/*Activity.*',
                                    '**/*AT.*',
                                    '**/*AT$.*',
                                    '**/*AT$*.*',
                                    '**/generated-sources/js2p/**'
                            ]

                            excludes.addAll(extension.extraJacocoExclusion)

                            // generated classes
                            classDirectories = fileTree(dir: "$buildDir/classes/", excludes: excludes)
                            additionalSourceDirs = files(sourceSets.main.allSource.srcDirs)
                        }
                    }
                }
            }

            //----------- Configuration for Sonar -------------
            configurePlugin(project, true, register, SonarQubePlugin) {
                project.configure(project) {
                    sonarqube {
                        properties {
                            property 'sonar.java.coveragePlugin', 'jacoco'
                            property 'sonar.jacoco.reportPaths', "$buildDir/jacoco/test.exec"
                            property 'sonar.coverage.jacoco.xmlReportPaths', "$buildDir/reports/jacoco/test/jacocoTestReport.xml"
                            property 'sonar.java.binaries', "$buildDir/classes"
                            property 'sonar.exclusions', "**/generated-sources/js2p/**"
                            property 'sonar.coverage.exclusions', "**/config/**,**/Application.**"
                        }
                    }
                }
            }

        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private void applyAndConfigureDockerPlugin(Project project, TechOpsGradlePluginExtension extension) {
        configurePlugin(project, true, extension.needs_Docker, PalantirDockerPlugin) {
            project.configure(project) {
                project.afterEvaluate {
                    docker {
                        dependsOn bootRepackage
                        name "$project.group/$project.name:" + project.version
                        dockerfile file("docker/Dockerfile")
                        def artifact = "$project.name-${project.version}.jar"
//                        files tasks.jar.outputs,
//                                "$projectDir/docker/entrypoint.sh",
//                                "$projectDir/docker/Dockerfile"
//
                        buildArgs(['JAR_FILE'  : "$project.buildDir/docker/*.jar",
                                   'ENTRYPOINT': "$project.buildDir/docker/entrypoint.sh"])
                        pull false
                        noCache true
                    }
                }
            }
        }
    }


    /**
     * Plugins may be registered manually and in this case plugin will also be configured, but only
     * when plugin support not disabled by pcs configuration. If plugin not registered and
     * sources auto detection allow registration - it will be registered and then configured.
     *
     * @param project project instance
     * @param enabled true if pcs plugin support enabled for plugin
     * @param register true to automatically register plugin
     * @param plugin plugin class
     * @param config plugin configuration closure
     */
    private void configurePlugin(Project project, boolean enabled, boolean register, Class plugin, Closure config) {
        if (!enabled) {
            // do not configure even if manually registered
            return
        } else if (register) {
            // register plugin automatically
            project.plugins.apply(plugin)
        }
        // configure plugin if registered (manually or automatic)
        project.plugins.withType(plugin) {
            config.call()
        }
    }
}