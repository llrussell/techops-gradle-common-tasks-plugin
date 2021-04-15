package org.techops.builder.gradle.plugin

import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

import org.eclipse.jgit.api.Git
//import org.gradle.internal.impldep.org.eclipse.jgit.api.Git
import org.gradle.api.internal.project.ProjectInternal

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.runners.MockitoJUnitRunner
import org.gradle.api.Project

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.fail



@RunWith(MockitoJUnitRunner.class)
class TechOpsGradlePluginTest {
//    @Mock

    @InjectMocks
    TechOpsGradlePluginTest pcsGradlePlugin

    public Git git

    public File repositoryFolder

    @Rule
    public TemporaryFolder fileFolder = new TemporaryFolder()


    @Before
    void setUp() {
        repositoryFolder = fileFolder.newFolder(".git")
        FileRepository repository = new FileRepositoryBuilder().setGitDir(repositoryFolder).build()
        repository.create()
        git = new Git(repository)
        Git.init().call()

    }

    @Test
    void testApply() {
        // Given
        Project project = ProjectBuilder.builder().withProjectDir(repositoryFolder).build()
        project.description = "something"

        // When
        project.getPluginManager().apply(TechOpsGradlePlugin.class)
        ((ProjectInternal) project).evaluate()

        // Then
    }


    @Test
    void testApplyNoDescription() {
        // Given
        Project project = ProjectBuilder.builder().withProjectDir(repositoryFolder).build()

        // When
        project.getPluginManager().apply(TechOpsGradlePlugin.class)

        try{
            ((ProjectInternal) project).evaluate()
            fail("Exception Not Thrown")
        } catch (Exception e) {
            assertNotNull(e)
        }

        // Then
    }


}
