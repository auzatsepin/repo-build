package repo.build

import junit.framework.Assert.assertEquals
import junit.framework.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File

class RepoManifestTestKt : BaseTestCaseKt() {

    @Before
    override fun setUp() {
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
    fun `should propogate component error`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")
        env.openManifest()
        try {
            RepoManifest.forEach(context,
                    ManifestAction(action = { _, project ->
                        if (project.attribute("path") == "c1") {
                            throw RepoBuildException("test")
                        }
                    })
            )
            fail()
        } catch (e: RepoBuildException) {
            assertEquals("Component c1 error test", e.message)
        }
    }

}