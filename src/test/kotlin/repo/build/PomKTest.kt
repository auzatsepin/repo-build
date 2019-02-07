package repo.build

import org.junit.Before
import org.junit.Test
import java.io.File

class PomKTest : BaseTestCase() {

    @Before
    fun `set up`() {
        super.setUp()
        sandboxKt = SandboxKt(RepoEnv(createTempDir()), options)
                .newGitComponent("c1")
                .newGitComponent("c2")
                .newGitComponent("manifest") { sandbox, dir ->
                    sandbox.gitInitialCommit(dir)
                    sandbox.buildManifest(dir)
                    Git.add(sandbox.context, dir, "default.xml")
                    Git.commit(sandbox.context, dir, "manifest")
                    sandbox
                }

    }

    @Test
    fun `should not found module in pom file`() {
        val url = File(sandboxKt.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")
        GitFeature.sync(context)

        val pomFile = File(sandboxKt.env.basedir, "pom.xml")
        Pom.generateXml(context, "master", pomFile)
        assertTrue(pomFile.exists())
        //todo jackson support for pom
        //assertEquals(0, pom.project.modules.module.findAll().size)
    }

    @Test
    fun `should found modules in pom file`() {
        sandboxKt.component("c1") {sandbox, dir ->
            val newFile = File(dir, "pom.xml")
            newFile.createNewFile()
            Git.add(sandbox.context, dir, "pom.xml")
            Git.commit(sandbox.context, dir, "pom")
            sandbox
        }

        val url = File(sandboxKt.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")
        GitFeature.sync(context)

        val pomFile = File(sandboxKt.env.basedir, "pom.xml")
        Pom.generateXml(context, "master", pomFile)
        assertTrue(pomFile.exists())
        //todo jackson support for pom
        //assertEquals(1, pom.modules.module.findAll().size)
    }

}