package repo.build

import com.google.common.io.Files
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.springframework.boot.test.rule.OutputCapture
import repo.build.filter.OutputFilter
import repo.build.filter.UnpushedStatusFilter
import java.io.File

@Suppress("UnstableApiUsage")
class GitFeatureTestKt : BaseTestCaseKt() {

    @get:Rule
    var outputCapture = OutputCapture()

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
    fun `should close manifest`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")
        env.openManifest()
        //todo manifest jackson
        //assertEquals(2, env.manifest.project.findAll().size)
        assertEquals("master", Git.getBranch(context, File(env.basedir, "manifest")))
    }

    @Test
    fun `should update manifest`() {
        val dir = File(env.basedir, "manifest")
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")
        // change manifest
        val newFile = File(url, "test")
        newFile.createNewFile()
        Git.add(context, url, "test")
        Git.commit(context, url, "test")

        GitFeature.updateManifest(context, Git.getBranch(context, dir))
        // check new file exists
        assertTrue(File(dir, "test").exists())
    }

    @Test
    fun `should sync all compoment from manifest`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        GitFeature.sync(context)
        assertEquals("master", Git.getBranch(context, File(env.basedir, "c1")))
        assertEquals("master", Git.getBranch(context, File(env.basedir, "c2")))
    }

    @Test
    fun `should check switch to not exist branch`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")
        assertEquals("master", Git.getBranch(context, File(env.basedir, "c1")))
        assertEquals("master", Git.getBranch(context, File(env.basedir, "c2")))
    }

    @Test
    fun `should switch to c1 branch`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        sandbox.component("c1") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "feature/1")
            sandbox
        }

        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")
        assertEquals("feature/1", Git.getBranch(context, File(env.basedir, "c1")))
        assertEquals("master", Git.getBranch(context, File(env.basedir, "c2")))
    }

    @Test
    fun `should switch feature`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        sandbox.component("c1") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "feature/1")
            Git.createBranch(sandbox.context, dir, "feature/2")
            sandbox
        }

        sandbox.component("c2") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "feature/1")
            sandbox
        }

        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")
        GitFeature.switch(context, "feature/2")
        assertEquals("master", Git.getBranch(context, File(env.basedir, "c2")))
    }

    @Test
    fun `should switch task`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        sandbox.component("c1") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "feature/1")
            Git.createBranch(sandbox.context, dir, "task/1")
            sandbox
        }

        sandbox.component("c2") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "feature/1")
            sandbox
        }

        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1", "task/1")
        assertEquals("task/1", Git.getBranch(context, File(env.basedir, "c1")))
    }

    @Test
    fun `should not switch task when feature doesn't exist`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        sandbox.component("c1") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "task/1")
            sandbox
        }

        sandbox.component("c2") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "feature/1")
            sandbox
        }

        GitFeature.sync(context)
        try {
            GitFeature.switch(context, "feature/1", "task/1")
            fail()
        } catch (e: Exception) {
            assertEquals("Component c1 error task task/1 exists but feature/1 not exists", e.message)
            assertEquals("master", Git.getBranch(context, File(env.basedir, "c1")))
        }
    }

    @Test
    fun `should merge feature to release`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        sandbox.component("c1") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "feature/1")
            val newFile = File(dir, "test")
            newFile.createNewFile()
            Git.add(sandbox.context, dir, "test")
            Git.commit(sandbox.context, dir, "test")
            sandbox
        }

        GitFeature.sync(context)

        GitFeature.releaseMergeFeature(context, "feature/1", true)

        assertEquals("prepareBuild", Git.getBranch(context, File(env.basedir, "c1")))
        assertTrue(File(File(env.basedir, "c1"), "test").exists())
        assertEquals("master", Git.getBranch(context, File(env.basedir, "c2")))
    }

    @Test
    fun `should release merge feature`() {
        val context = ActionContext(env, null, options, DefaultActionHandler())
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        sandbox.component("c1") { sandbox, dir ->
            Git.createBranch(context, dir, "feature/1")
            val newFile = File(dir, "test")
            newFile.createNewFile()
            Git.add(context, dir, "test")
            Git.commit(context, dir, "test")
            sandbox
        }

        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")

        GitFeature.featureMergeRelease(context, "feature/1")

        assertEquals("feature/1", Git.getBranch(context, File(env.basedir, "c1")))
        assertTrue(File(File(env.basedir, "c1"), "test").exists())
        assertEquals("master", Git.getBranch(context, File(env.basedir, "c2")))
    }

    @Test
    fun `should merge feature to task`() {
        val context = ActionContext(env, null, options, DefaultActionHandler())
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        sandbox.component("c1") { sandbox, dir ->
            // create task branch
            val c1Dir = File(sandbox.env.basedir, "c1")
            Git.createBranch(context, c1Dir, "task/1")
            Git.createBranch(context, dir, "feature/1")
            Git.checkout(context, dir, "feature/1")
            val newFile = File(dir, "test")
            newFile.createNewFile()
            newFile.text = "test"
            Git.add(context, dir, "test")
            Git.commit(context, dir, "test")
            sandbox
        }

        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1", "task/1")

        GitFeature.taskMergeFeature(context, "task/1", "feature/1")

        val c1Dir = File(env.basedir, "c1")
        val testFile = File(c1Dir, "test")

        assertEquals("task/1", Git.getBranch(context, File(env.basedir, "c1")))
        assertEquals("test", testFile.text)
        assertTrue(File(File(env.basedir, "c1"), "test").exists())
        assertEquals("master", Git.getBranch(context, File(env.basedir, "c2")))
    }

    @Test
    fun `should write changes to output`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        GitFeature.sync(context)

        val c1Dir = File(env.basedir, "c1")
        val newFile = File(c1Dir, "new")
        newFile.text = "new"

        val unpushedFile = File(c1Dir, "unpushed")
        unpushedFile.text = "unpushed"
        Git.add(context, c1Dir, "unpushed")
        Git.commit(context, c1Dir, "unpushed")

        outputCapture.reset()

        GitFeature.status(context)
        val splitedOutput = outputCapture.toString().split("\n")
        assertEquals("should contain 1 c1", 1,
                getByValueFromOutput("c1", splitedOutput).size)
        assertEquals("should contain 2 master branch", 2,
                getByValueFromOutput("master", splitedOutput).size)
        assertEquals("should contain 1 new file", 1,
                getByValueFromOutput("?? new", splitedOutput).size)
        assertEquals("should contain 1 c2 ", 1,
                getByValueFromOutput("c2", splitedOutput).size)
        assertEquals("should contain 1 empty string", 1,
                getByValueFromOutput("", splitedOutput).size)
        assertEquals("should contain 2 remote ref repository name", 2,
                containsValueFromFromOutput("refs/remotes/origin/master", splitedOutput).size)
        assertEquals("should contain 1 unpushed", 1,
                containsValueFromFromOutput("unpushed", splitedOutput).size)

        outputCapture.reset()

        val predicates = mutableListOf<OutputFilter>()
        predicates.add(UnpushedStatusFilter())
        context.outputFilter[GitFeature.ACTION_STATUS] = predicates
        GitFeature.status(context)
        val splitedOutput1 = outputCapture.toString().split("\n")

        assertEquals(5, splitedOutput1.size)
        assertEquals("should contain 1 c1", 1,
                getByValueFromOutput("c1", splitedOutput1).size)
        assertEquals("should contain 1 master branch", 1,
                getByValueFromOutput("master", splitedOutput1).size)
        assertEquals("should contain 1 new file", 1,
                getByValueFromOutput("?? new", splitedOutput1).size)
        assertEquals("should contain 1 remote ref repository name", 1,
                containsValueFromFromOutput("refs/remotes/origin/master", splitedOutput1).size)
        assertEquals("should contain 1 unpushed", 1,
                containsValueFromFromOutput("unpushed", splitedOutput1).size)
    }

    @Test
    fun `should show unpushed branches`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        GitFeature.sync(context)

        val c1Dir = File(env.basedir, "c1")

        Git.createBranch(context, c1Dir, "newBranch")
        Git.checkout(context, c1Dir, "newBranch")

        val newFile = File(c1Dir, "new")
        newFile.text = "new"

        val unpushedFile = File(c1Dir, "unpushed")
        unpushedFile.text = "unpushed"
        Git.add(context, c1Dir, "unpushed")
        Git.commit(context, c1Dir, "unpushed")

        outputCapture.reset()

        GitFeature.status(context)
        val splitedOutput = outputCapture.toString().split("\n")
        assertEquals("should contain 1 c1", 1,
                getByValueFromOutput("c1", splitedOutput).size)
        assertEquals("should contain 1 master branch", 1,
                getByValueFromOutput("master", splitedOutput).size)
        assertEquals("should contain new file", 1,
                getByValueFromOutput("?? new", splitedOutput).size)
        assertEquals("should contain 1 c2 ", 1,
                getByValueFromOutput("c2", splitedOutput).size)
        assertEquals("should contain 1 remote ref repository name", 1,
                containsValueFromFromOutput("refs/remotes/origin/master", splitedOutput).size)
        assertEquals("should contain 1 Branch not pushed", 1,
                containsValueFromFromOutput("Branch not pushed", splitedOutput).size)

        outputCapture.reset()

        val predicates = mutableListOf<OutputFilter>()
        predicates.add(UnpushedStatusFilter())
        context.outputFilter[GitFeature.ACTION_STATUS] = predicates
        GitFeature.status(context)

        val splitedOutput1 = outputCapture.toString().split("\n")
        assertEquals("should contain 1 c1", 1,
                getByValueFromOutput("c1", splitedOutput1).size)
        assertEquals("should contain 1 newBranch", 1,
                getByValueFromOutput("newBranch", splitedOutput1).size)
        assertEquals("should contain 1 new file", 1,
                getByValueFromOutput("?? new", splitedOutput1).size)
        assertEquals("should contain 1 empty string", 1,
                getByValueFromOutput("", splitedOutput1).size)
        assertEquals("should contain 1 Branch not pushed", 1,
                containsValueFromFromOutput("Branch not pushed", splitedOutput1).size)
    }

    @Test
    fun `should grep all components`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        sandbox.component("c1") { sandbox, dir ->
            val newFile = File(dir, "test")
            newFile.createNewFile()
            newFile.text = "TEST123"
            Git.add(sandbox.context, dir, "test")
            Git.commit(sandbox.context, dir, "test")
            sandbox
        }

        GitFeature.sync(context)

        val result = GitFeature.grep(context, "123")
        assertEquals("test:TEST123\n", result["c1"])
        assertEquals("", result["c2"])

    }

    @Test
    fun `should stash changes`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        sandbox.component("c1") { sandbox, dir ->
            val newFile = File(dir, "test")
            newFile.createNewFile()
            newFile.text = "TEST123"
            Git.add(sandbox.context, dir, "test")
            Git.commit(sandbox.context, dir, "test")
            sandbox
        }

        GitFeature.sync(context)

        val file = File(env.basedir, "c1/test")
        assertEquals("TEST123", file.text)
        // update file
        file.text = "123TEST"
        GitFeature.stash(context)
        assertEquals("TEST123", file.text)
        GitFeature.stashPop(context)
        assertEquals("123TEST", file.text)
    }

    @Test
    fun `should push feature`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        sandbox.component("c1") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "feature/1")
            sandbox
        }

        GitFeature.sync(context)
        GitFeature.switch(context, "feature/1")

        // modify branch
        val c1Dir = File(env.basedir, "c1")
        val c1File = File(c1Dir, "README.md")
        c1File.text = "update"
        Git.add(context, c1Dir, "README.md")
        Git.commit(context, c1Dir, "update")

        // create branch
        val c2Dir = File(env.basedir, "c2")
        Git.createBranch(context, c2Dir, "feature/1")
        Git.checkout(context, c2Dir, "feature/1")
        val c2File = File(c2Dir, "README.md")
        c2File.text = "update"
        Git.add(context, c2Dir, "README.md")
        Git.commit(context, c2Dir, "update")

        GitFeature.pushFeatureBranch(context, "feature/1", true)

        sandbox.component("c1") { sandbox, dir ->
            Git.checkout(sandbox.context, dir, "feature/1")
            assertEquals("update", File(dir, "README.md").text)
            sandbox
        }

        sandbox.component("c2") { sandbox, dir ->
            Git.checkout(sandbox.context, dir, "feature/1")
            assertEquals("update", File(dir, "README.md").text)
            sandbox
        }
    }

    @Test
    fun `should push manifest`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        sandbox.component("c1") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "master1")
            Git.checkout(sandbox.context, dir, "master1")
            sandbox
        }

        sandbox.component("c2") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "master1")
            Git.checkout(sandbox.context, dir, "master1")
            sandbox
        }

        GitFeature.sync(context)

        // modify branch
        val c1Dir = File(env.basedir, "c1")
        val c1File = File(c1Dir, "README.md")
        c1File.text = "update"
        Git.add(context, c1Dir, "README.md")
        Git.commit(context, c1Dir, "update")

        // create branch
        val c2Dir = File(env.basedir, "c2")
        val c2File = File(c2Dir, "README.md")
        c2File.text = "update"
        Git.add(context, c2Dir, "README.md")
        Git.commit(context, c2Dir, "update")

        GitFeature.pushManifestBranch(context, true)

        sandbox.component("c1") { sandbox, dir ->
            Git.checkout(sandbox.context, dir, "master")
            assertEquals("update", File(dir, "README.md").text)
            sandbox
        }

        sandbox.component("c2") { sandbox, dir ->
            Git.checkout(sandbox.context, dir, "master")
            assertEquals("update", File(dir, "README.md").text)
            sandbox
        }

    }

    @Test
    fun `should push tag`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        GitFeature.sync(context)

        GitFeature.addTagToCurrentHeads(context, "1")
        GitFeature.pushTag(context, "1")

        sandbox.component("c1") { sandbox, dir ->
            assertTrue(Git.tagPresent(sandbox.context, dir, "1"))
            sandbox
        }

        sandbox.component("c2") { sandbox, dir ->
            assertTrue(Git.tagPresent(sandbox.context, dir, "1"))
            sandbox
        }

    }

    @Test
    fun `should checkout tag`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        GitFeature.sync(context)

        GitFeature.addTagToCurrentHeads(context, "1")
        GitFeature.pushTag(context, "1")

        GitFeature.sync(context)

        // modify branch
        val c1Dir = File(env.basedir, "c1")
        val c1File = File(c1Dir, "README.md")
        c1File.text = "update"
        Git.add(context, c1Dir, "README.md")
        Git.commit(context, c1Dir, "update")

        GitFeature.checkoutTag(context, "1")

        assertEquals("", File(c1Dir, "README.md").text)
    }

    @Test
    fun `should checkout tag with new component`() {
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        GitFeature.sync(context)

        GitFeature.addTagToCurrentHeads(context, "1")
        GitFeature.pushTag(context, "1")

        sandbox.newGitComponent("c3") { sandbox, dir ->
            sandbox.buildManifest(dir)
            Git.add(sandbox.context, dir, "default.xml")
            Git.commit(sandbox.context, dir, "add_c3")
            sandbox
        }

        GitFeature.sync(context)

        // modify branch
        val c1Dir = File(env.basedir, "c1")
        val c1File = File(c1Dir, "README.md")
        c1File.text = "update"
        Git.add(context, c1Dir, "README.md")
        Git.commit(context, c1Dir, "update")

        GitFeature.checkoutTag(context, "1")

        assertEquals("", File(c1Dir, "README.md").text)
    }

    @Ignore //todo need rewrite main code
    @Test
    fun `should merge release to release`() {
        //init
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        sandbox.component("c2") { sandbox, dir ->
            Git.createBranch(context, dir, "develop/1.0")
            Git.createBranch(context, dir, "develop/2.0")

            //some changes
            Git.checkout(context, dir, "develop/1.0")
            val newFile = File(dir, "test")
            newFile.createNewFile()
            newFile.text = "TEST123"
            Git.add(context, dir, "test")
            Git.commit(context, dir, "test")
            sandbox
        }

        sandbox.component("manifest") { sandbox, dir ->
            //change default branch to develop/1.0 on c2 component in manifest
            Git.createBranch(context, dir, "1.0")
            Git.checkout(context, dir, "1.0")
            sandbox.changeDefaultBranchComponentOnManifest(dir, "c2", "develop/1.0")
            Git.add(context, dir, "default.xml")
            Git.commit(context, dir, "vup")

            //change default branch to develop/2.0 on c2 component in manifest
            Git.createBranch(context, dir, "2.0")
            Git.checkout(context, dir, "2.0")
            sandbox.changeDefaultBranchComponentOnManifest(dir, "c2", "develop/2.0")
            Git.add(context, dir, "default.xml")
            Git.commit(context, dir, "vup")
            sandbox
        }

        //expected call function
        GitFeature.releaseMergeRelease(context, "1.0", "2.0", "/(\\ d +\\.\\ d +)/", null)
        /*{
            List list -> return list[0]+".0"
        })*/

        Git.checkout(context, File(context.env.basedir, "c2"), "develop/2.0")
        assertEquals("TEST123", File(context.env.basedir, "c2/test").text)
    }

    @Test
    fun `should create bundles from manifest`() {

        sandbox.component("c1") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "origin/master")
            sandbox
        }

        sandbox.component("c2") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "origin/master")
            sandbox
        }

        val bundleDir = Files.createTempDir()
        sandbox.context.env.openManifest()
        GitFeature.createManifestBundles(sandbox.context, bundleDir)

        val c1bundle = File(bundleDir, "c1")
        val c2bundle = File(bundleDir, "c1")

        assertTrue(c1bundle.canRead())
        assertTrue(c2bundle.canRead())
    }

    @Test
    fun `should clone from bundle`() {

        sandbox.component("c1") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "1.0")
            val newFile = File(dir, "test")
            newFile.createNewFile()
            newFile.text = "TEST123"
            Git.add(context, dir, "test")
            Git.commit(context, dir, "test_1.0")
            sandbox
        }

        sandbox.component("c2") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "1.5")
            val newFile = File(dir, "test")
            newFile.createNewFile()
            newFile.text = "Launch2"
            Git.add(context, dir, "test")
            Git.commit(context, dir, "test_1.5")
            sandbox
        }

        sandbox.component("manifest") { sandbox, dir ->
            Git.createBranch(context, dir, "1.0")
            Git.checkout(context, dir, "1.0")
            sandbox.changeDefaultBranchComponentOnManifest(dir, "c1", "1.0")
            Git.add(context, dir, "default.xml")
            Git.commit(context, dir, "vup")

            Git.createBranch(context, dir, "1.5")
            Git.checkout(context, dir, "1.5")
            sandbox.changeDefaultBranchComponentOnManifest(dir, "c2", "1.5")
            Git.add(context, dir, "default.xml")
            Git.commit(context, dir, "vup")
            sandbox
        }

        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "1.5")

        GitFeature.sync(context)

        //export bundles
        val bundleDir = Files.createTempDir()
        context.env.openManifest()
        GitFeature.createBundleForManifest(context, bundleDir, "manifest.bundle")
        GitFeature.createManifestBundles(context, bundleDir)

        //new sandbox
        super.setUp()

        //clone manifest from bundle
        GitFeature.cloneOrUpdateFromBundle(context, bundleDir, "manifest", "manifest.bundle", "1.5")

        context.env.openManifest()
        GitFeature.cloneOrUpdateFromBundles(context, bundleDir)

        assertTrue(File(context.env.basedir, "manifest").isDirectory)
        assertTrue(File(context.env.basedir, "c1").isDirectory)
        assertTrue(File(context.env.basedir, "c2").isDirectory)
        assertEquals("1.5", Git.getBranch(context, File(context.env.basedir, "manifest")))
        assertEquals("1.0", Git.getBranch(context, File(context.env.basedir, "c1")))
        assertEquals("1.5", Git.getBranch(context, File(context.env.basedir, "c2")))
    }

    @Ignore //todo should rewrite main code
    @Test
    fun `should get last commits from manifest`() {
/*        val commits = mutableListOf<String>()
        sandbox.env.openManifest()
        RepoManifest.forEach(sandbox.context, {
            ActionContext actionContext, Node project ->
            commits.add(Git.getLastCommit(actionContext, File(sandbox.env.basedir, project.@ path)))
        })

        assertEquals(2, commits.size())*/
    }

    @Test
    fun `should create delta bundle`() {
        sandbox.component("c1") { sandbox, dir ->
            Git.createBranch(sandbox.context, dir, "1.0")
            Git.checkout(sandbox.context, dir, "1.0")
            val newFile = File(dir, "test")
            newFile.createNewFile()
            newFile.text = "TEST123"
            Git.add(sandbox.context, dir, "test")
            Git.commit(sandbox.context, dir, "test_1.0")
            val firstCommit = Git.getLastCommit(sandbox.context, dir)
            val newFile2 = File(dir, "test2")
            newFile2.createNewFile()
            newFile2.text = "AAAAA"
            Git.add(sandbox.context, dir, "test2")
            Git.commit(sandbox.context, dir, "test_2.0")
            val bundleFile = File.createTempFile("repo-build-test", ".bundle")
            Git.createFeatureBundle(sandbox.context, "1.0", dir, bundleFile, firstCommit)

            try {
                Git.clone(sandbox.context, bundleFile.absolutePath, "origin", File(sandbox.env.basedir, "clone"))
            } catch (e: RepoBuildException) {
            }
            sandbox
        }
    }

    @Test
    fun `should update from bundle`() {
        //clone project
        val url = File(sandbox.env.basedir, "manifest")
        GitFeature.cloneManifest(context, url.absolutePath, "master")

        GitFeature.sync(context)

        //export bundles
        val bundleDir1 = Files.createTempDir()
        context.env.openManifest()
        GitFeature.createBundleForManifest(context, bundleDir1, "manifest.bundle")
        GitFeature.createManifestBundles(context, bundleDir1)

        sandbox.component("c1") { sandbox, dir ->
            val newFile = File(dir, "test")
            newFile.createNewFile()
            newFile.text = "TEST123"
            Git.add(context, dir, "test")
            Git.commit(context, dir, "test_1.0")
            sandbox
        }

        sandbox.component("c2") { sandbox, dir ->
            val newFile = File(dir, "test")
            newFile.createNewFile()
            newFile.text = "Launch2"
            Git.add(context, dir, "test")
            Git.commit(context, dir, "test_1.5")
            sandbox
        }

        GitFeature.sync(context)

        //export bundles
        val bundleDir2 = Files.createTempDir()
        GitFeature.createBundleForManifest(context, bundleDir2, "manifest.bundle")
        GitFeature.createManifestBundles(context, bundleDir2)

        //new sandbox
        super.setUp()

        //import bundles
        GitFeature.cloneOrUpdateFromBundle(context, bundleDir1, "manifest", "manifest.bundle", "master")

        context.env.openManifest()
        GitFeature.cloneOrUpdateFromBundles(context, bundleDir1)

        val res = ExecuteProcess.executeCmd0(context, File(context.env.basedir, "c1"), "git log --oneline -n 1", true)
        val (_, log) = res.split(" ").map { it.trim() }
        res.split(" ")
        assertEquals("\"init\"", log)

        GitFeature.cloneOrUpdateFromBundle(context, bundleDir2, "manifest", "manifest.bundle", "master")
        GitFeature.cloneOrUpdateFromBundles(context, bundleDir2)
        val res1 = ExecuteProcess.executeCmd0(context, File(context.env.basedir, "c1"), "git log --oneline -n 1", true)
        val (_, log1) = res1.split(" ").map { it.trim() }
        assertEquals("\"test_1.0\"", log1)
    }

    @Test
    fun `should create bundle from manifest`() {
        GitFeature.createBundleForManifest(sandbox.context, sandbox.context.env.basedir.absoluteFile, "manifest.bundle")
        assertTrue(File(sandbox.context.env.basedir, "manifest.bundle").canRead())
    }

    private fun getByValueFromOutput(value: String, output: List<String>): List<String> {
        if (output.isEmpty()) return listOf()
        return output.filter {
            it == value
        }
    }

    private fun containsValueFromFromOutput(value: String, output: List<String>): List<String> {
        if (output.isEmpty()) return listOf()
        return output.filter { it.contains(value) }
    }

}

private var File.text: String
    get() {
        return readText(Charsets.UTF_8)
    }
    set(text) {
        writeText(text, Charsets.UTF_8)
    }
