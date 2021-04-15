package org.techops.builder.gradle.extension

class TechOpsGradlePluginExtension {

    int commitCount
    int majorOffset
    int patchOffset
    int localChangesCount
    boolean is_GroovyProject = true
    boolean is_JavaProject = true
    boolean is_KotlinProject
    boolean is_PythonProject
    boolean needs_Springboot
    boolean needs_Docker

    Set<String> extraJacocoExclusion = []
}
