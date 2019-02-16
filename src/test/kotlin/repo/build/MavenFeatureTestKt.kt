package repo.build

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileReader
import java.util.*

class MavenFeatureTestKt : BaseTestCaseKt() {

    @Before
    override fun setUp() {
        super.setUp()
        sandbox = SandboxKt(RepoEnv(createTempDir()), options)
                .newGitComponent("parent") { sandbox, dir ->
                    File("src/test/resources/parent").copyRecursively(dir)
                    Git.add(sandbox.context, dir, "*.*")
                    Git.commit(sandbox.context, dir, "add")
                    Git.createBranch(sandbox.context, dir, "feature/1")
                    sandbox
                }
                .newGitComponent("parent2") { sandbox, dir ->
                    File("src/test/resources/parent2").copyRecursively(dir)
                    Git.add(sandbox.context, dir, "*.*")
                    Git.commit(sandbox.context, dir, "add")
                    Git.createBranch(sandbox.context, dir, "feature/1")
                    sandbox
                }
                .newGitComponent("c1") { sandbox, dir ->
                    File("src/test/resources/c1").copyRecursively(dir)
                    Git.add(sandbox.context, dir, "*.*")
                    Git.commit(sandbox.context, dir, "add")
                    Git.createBranch(sandbox.context, dir, "feature/1")
                    sandbox
                }
                .newGitComponent("c2") { sandbox, dir ->
                    File("src/test/resources/c2").copyRecursively(dir)
                    Git.add(sandbox.context, dir, "*.*")
                    Git.commit(sandbox.context, dir, "add")
                    Git.createBranch(sandbox.context, dir, "feature/1")
                    sandbox
                }
                .newGitComponent("c3") { sandbox, dir ->
                    File("src/test/resources/c3").copyRecursively(dir)
                    Git.add(sandbox.context, dir, "*.*")
                    Git.commit(sandbox.context, dir, "add")
                    Git.createBranch(sandbox.context, dir, "feature/1")
                    sandbox
                }
                .newGitComponent("c4") { sandbox, dir ->
                    File("src/test/resources/c4").copyRecursively(dir)
                    Git.add(sandbox.context, dir, "*.*")
                    Git.commit(sandbox.context, dir, "add")
                    Git.createBranch(sandbox.context, dir, "feature/1")
                    sandbox
                }
                .newGitComponent("manifest") { sandbox, dir ->
                    sandbox.gitInitialCommit(dir)
                    sandbox.buildManifest(dir)
                    Git.add(sandbox.context, dir, "default.xml")
                    Git.commit(sandbox.context, dir, "manifest")
                    sandbox
                }
    }

    @Test
    fun `should update versions`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        // execute parent
        cleanInstallParent()
        // update c1 version to 1.1.0-SNAPSHOT on master
        sandbox.component("c1") { sandbox, dir ->
            Maven.execute(sandbox.context, File(dir, "pom.xml"), MavenRequest { req ->
                req.goals = Arrays.asList("versions:set")
                req.isInteractive = false
                val properties = Properties()
                properties["newVersion"] = "1.1.0-SNAPSHOT"
                properties["generateBackupPoms"] = "false"
                req.properties = properties
            })
            Git.addUpdated(sandbox.context, dir)
            Git.commit(sandbox.context, dir, "vup")
            sandbox
        }
        // update c2 version to 2.1.0-SNAPSHOT on master
        sandbox.component("c2") { sandbox, dir ->
            Maven.execute(sandbox.context, File(dir, "pom.xml"), MavenRequest { req ->
                req.goals = Arrays.asList("versions:set")
                req.isInteractive = false
                val properties = Properties()
                properties["newVersion"] = "2.1.0-SNAPSHOT"
                properties["generateBackupPoms"] = "false"
                req.properties = properties
            })
            Git.addUpdated(sandbox.context, dir)
            Git.commit(sandbox.context, dir, "vup")
            sandbox
        }

        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")
        GitFeature.featureMergeRelease(context, "feature/1")

        MavenFeature.buildParents(context)

        MavenFeature.updateVersions(context, "feature/1", "test.repo-build:*", null, true)

        // check parent version
        val c2Pom = mavenReader.read(FileReader(File(env.basedir, "c2/pom.xml")))
        assertEquals("1.1.0-SNAPSHOT", c2Pom.properties.getProperty("c1.version"))
    }

    @Test
    fun `should update version continue from component`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        // execute parent
        cleanInstallParent()
        sandbox.component("c1") { sandbox, dir ->
            Maven.execute(sandbox.context, File(dir, "pom.xml"), MavenRequest { req ->
                req.goals = Arrays.asList("versions:set")
                req.isInteractive = false
                val properties = Properties()
                properties["newVersion"] = "1.1.0-SNAPSHOT"
                properties["generateBackupPoms"] = "false"
                req.properties = properties
            })
            Git.addUpdated(sandbox.context, dir)
            Git.commit(sandbox.context, dir, "vup")
            sandbox
        }
        // update c2 version to 2.1.0-SNAPSHOT on master

        sandbox.component("c2") { sandbox, dir ->
            Maven.execute(sandbox.context, File(dir, "pom.xml"), MavenRequest { req ->
                req.goals = Arrays.asList("versions:set")
                req.isInteractive = false
                val properties = Properties()
                properties["newVersion"] = "2.1.0-SNAPSHOT"
                properties["generateBackupPoms"] = "false"
                req.properties = properties
            })
            Git.addUpdated(sandbox.context, dir)
            Git.commit(sandbox.context, dir, "vup")
            sandbox
        }

        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")
        GitFeature.featureMergeRelease(context, "feature/1")

        MavenFeature.buildParents(context)

        MavenFeature.updateVersions(context, "feature/1", "test.repo-build:*", null, true)

        sandbox.component("c1") { sandbox, dir ->
            Maven.execute(sandbox.context, File(dir, "pom.xml"), MavenRequest { req ->
                req.goals = Arrays.asList("clean")
                req.isInteractive = false
            })
            sandbox
        }

        MavenFeature.updateVersions(context, "feature/1", "test.repo-execute:*", "c2", true)

        // check parent version
        val c2Pom = mavenReader.read(FileReader(File(env.basedir, "c2/pom.xml")))
        assertEquals("1.1.0-SNAPSHOT", c2Pom.properties.getProperty("c1.version"))

        val c1Target = File(env.basedir, "c1/target")
        assertFalse(c1Target.exists())

    }


    @Test
    fun `should change parent version after release`() {

        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        // install parent 1.0.0-SNAPSHOT
        cleanInstallParent()

        updateInstallParent("1.1.0")
        updateInstallParent("1.1.1")
        updateInstallParent("1.1.2-SNAPSHOT")

        GitFeature.sync(context)

        MavenFeature.updateReleaseParent(context, "parent", false, false)

        val c1Model = mavenReader.read(FileReader(File(env.basedir, "c1/pom.xml")))
        assertEquals("1.1.1", c1Model.parent.version)
        val c2Model = mavenReader.read(FileReader(File(env.basedir, "c2/pom.xml")))
        assertEquals("1.1.1", c2Model.parent.version)
        val c3Model = mavenReader.read(FileReader(File(env.basedir, "c3/pom.xml")))
        assertEquals("1.1.1", c3Model.parent.version)
    }

    @Test
    fun `should get component map`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")
        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")
        Pom.generateXml(context, "feature/1", File(env.basedir, "pom.xml"))

        val componentsMap = ComponentDependencyGraph.getModuleToComponentMap(MavenFeature.getComponents(context))
        assertEquals(10, componentsMap.size)
    }

    @Test
    fun `should sort components`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")
        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")
        Pom.generateXml(context, "feature/1", File(env.basedir, "pom.xml"))

        val components = MavenFeature.getComponents(context)
        val sortedComponents = MavenFeature.sortComponents(components)
        assertEquals(6, sortedComponents.size)
        assertEquals("parent", sortedComponents[0].artifactId)
        assertEquals("c1-parent", sortedComponents[1].artifactId)
        assertEquals("c2-parent", sortedComponents[2].artifactId)
        assertEquals("parent2", sortedComponents[3].artifactId)
        assertEquals("c4-parent", sortedComponents[4].artifactId)
        assertEquals("c3-parent", sortedComponents[5].artifactId)
    }

    @Test
    fun `should sort parent component`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")
        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")
        Pom.generateXml(context, "feature/1", File(env.basedir, "pom.xml"))

        val components = MavenFeature.getParentComponents(MavenFeature.getComponents(context))

        val sortedComponents = MavenFeature.sortComponents(components)
        assertEquals(2, sortedComponents.size)
        assertEquals("parent", sortedComponents[0].artifactId)
        assertEquals("parent2", sortedComponents[1].artifactId)
    }

    @Test
    fun `should build parent`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")
        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")
        Pom.generateXml(context, "feature/1", File(env.basedir, "pom.xml"))

        MavenFeature.buildParents(context)
        @Suppress("UNCHECKED_CAST")
        Maven.execute(context, File(env.basedir, "pom.xml"), mutableListOf("clean", "install"), Properties() as MutableMap<String, String>)
    }

    @Test
    fun `should build parallel`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")
        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")

        assertTrue(MavenFeature.buildParallel(context))
    }

    @Test
    fun `parallel build should fail`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")
        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")

        // create class with syntax error
        File(context.env.basedir, "c2/api/src/main/java/Test.java").text = "bla class"

        assertFalse(MavenFeature.buildParallel(context))
    }

    @Test
    fun `parallel build project with circular dependency should fail`() {
        val url = File(sandbox.env.basedir, "manifest")

        sandbox.component("c1") { sandbox, dir ->
            Git.checkout(this.sandbox.context, dir, "feature/1")
            File("src/test/resources/circular/c1").copyRecursively(dir, true)
            Git.add(sandbox.context, dir, "*.*")
            Git.commit(sandbox.context, dir, "add")
            sandbox
        }


        GitFeature.cloneManifest(context, url.absolutePath, "master")
        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")

        try {
            MavenFeature.buildParallel(context)
            fail()
        } catch (ignore: Exception) {

        }
    }

    @Test
    fun `should update feature parent`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        // execute parent
        cleanInstallParent()

        updateInstallParent("1.1.0-SNAPSHOT")
        updateInstallParent("1.1.1-SNAPSHOT")


        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")
        GitFeature.featureMergeRelease(context, "feature/1")

        MavenFeature.updateFeatureParent(context, "feature/1", "parent", true, true)

        // check parent version
        val c1Pom = mavenReader.read(FileReader(File(env.basedir, "c1/pom.xml")))
        assertEquals("1.1.1-SNAPSHOT", c1Pom.parent.version)
        val c2Pom = mavenReader.read(FileReader(File(env.basedir, "c2/pom.xml")))
        assertEquals("1.1.1-SNAPSHOT", c2Pom.parent.version)
        val c3Pom = mavenReader.read(FileReader(File(env.basedir, "c3/pom.xml")))
        assertEquals("1.1.1-SNAPSHOT", c3Pom.parent.version)
    }


    private fun updateInstallParent(version: String) {
        sandbox.component("parent") { sandbox, dir ->
            Maven.execute(this.sandbox.context, File(dir, "pom.xml"), MavenRequest { req ->
                req.goals = Arrays.asList("versions:set")
                req.isInteractive = false
                val properties = Properties()
                properties["newVersion"] = version
                properties["generateBackupPoms"] = "false"
                req.properties = properties
            })
            Git.add(sandbox.context, dir, "pom.xml")
            Git.commit(sandbox.context, dir, "vup")
            sandbox
        }
        cleanInstallParent()
    }

    private fun cleanInstallParent(): SandboxKt {
        sandbox.component("parent") { sandbox, dir ->
            Maven.execute(sandbox.context, File(dir, "pom.xml"), MavenRequest { req ->
                req.goals = Arrays.asList("clean", "install")
                req.isInteractive = false
                val properties = Properties()
                properties["skipTests"] = "true"
                req.properties = properties
            })
            sandbox
        }
        return sandbox
    }

}