package repo.build

import kotlin.Unit
import kotlin.jvm.functions.Function2
import org.junit.Before
import org.junit.Test

/**
 */
class RepoManifestTest extends BaseTestCase {

    @Before
    void setUp() throws Exception {
        super.setUp()
        sandbox = new Sandbox(new RepoEnv(createTempDir()), options)
                .newGitComponent('c1')
                .newGitComponent('c2')
                .newGitComponent('manifest', new SandboxClosure(
                new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        sandbox.gitInitialCommit(dir)
                        sandbox.buildManifest(dir)
                        Git.add(sandbox.context, dir, 'default.xml')
                        Git.commit(sandbox.context, dir, 'manifest')
                        return sandbox
                    }
                }
        ))
    }

    @Test
    void testPropogateComponentError() {
        def url = new File(sandbox.env.basedir, 'manifest')
        GitFeature.cloneManifest(context, url.getAbsolutePath(), 'master')
        env.openManifest()
        try {
            RepoManifest.forEach(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            if (project.@path == 'c1') {
                                throw new RepoBuildException('test')
                            }
                            return Unit.INSTANCE
                        }
                    }))
            fail()
        }
        catch (RepoBuildException e) {
            assertEquals('Component c1 error test', e.message)
        }
    }


}