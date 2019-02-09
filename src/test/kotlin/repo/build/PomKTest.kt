package repo.build

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileReader

class PomKTest : BaseTestCaseKt() {

    @Before
    fun `set up`() {
        super.setUp()
        sandbox = SandboxKt(RepoEnv(createTempDir()), options)
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
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")
        GitFeature.sync(context)

        val pomFile = File(sandbox.env.basedir, "pom.xml")
        Pom.generateXml(context, "master", pomFile)
        assertTrue(pomFile.exists())
        val model = mavenReader.read(FileReader(pomFile))
        assertEquals(0, model.modules.size)
    }

    @Test
    fun `should found modules in pom file`() {
        sandbox.component("c1") {sandbox, dir ->
            val newFile = File(dir, "pom.xml")
            newFile.createNewFile()
            Git.add(sandbox.context, dir, "pom.xml")
            Git.commit(sandbox.context, dir, "pom")
            sandbox
        }

        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")
        GitFeature.sync(context)

        val pomFile = File(sandbox.env.basedir, "pom.xml")
        Pom.generateXml(context, "master", pomFile)
        assertTrue(pomFile.exists())
        val model = mavenReader.read(FileReader(pomFile))
        assertEquals(1, model.modules.size)
    }

}