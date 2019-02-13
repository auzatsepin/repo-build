package repo.build

import org.junit.Before
import org.junit.Test
import java.io.File

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


}