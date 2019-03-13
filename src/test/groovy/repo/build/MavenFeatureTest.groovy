package repo.build

import kotlin.Unit
import kotlin.jvm.functions.Function1
import kotlin.jvm.functions.Function2
import org.apache.maven.shared.invoker.InvocationRequest
import org.junit.Before
import org.junit.Test

class MavenFeatureTest extends BaseTestCase {

    @Before
    void setUp() throws Exception {
        super.setUp()
        sandbox = new Sandbox(new RepoEnv(createTempDir()), options)
                .newGitComponent('parent',
                new SandboxClosure(new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        def ant = new AntBuilder()
                        ant.copy(todir: dir) {
                            fileset(dir: 'src/test/resources/parent') {
                                include(name: '**/**')
                            }
                        }
                        Git.add(sandbox.context, dir, '*.*')
                        Git.commit(sandbox.context, dir, 'add')
                        Git.createBranch(sandbox.context, dir, 'feature/1')
                        return sandbox
                    }
                })
        )
                .newGitComponent('parent2',
                new SandboxClosure(new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        def ant = new AntBuilder()
                        ant.copy(todir: dir) {
                            fileset(dir: 'src/test/resources/parent2') {
                                include(name: '**/**')
                            }
                        }
                        Git.add(sandbox.context, dir, '*.*')
                        Git.commit(sandbox.context, dir, 'add')
                        Git.createBranch(sandbox.context, dir, 'feature/1')
                        return sandbox
                    }
                }))
                .newGitComponent('c1',
                new SandboxClosure(new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        def ant = new AntBuilder()
                        ant.copy(todir: dir) {
                            fileset(dir: 'src/test/resources/c1') {
                                include(name: '**/**')
                            }
                        }
                        Git.add(sandbox.context, dir, '*.*')
                        Git.commit(sandbox.context, dir, 'add')
                        Git.createBranch(sandbox.context, dir, 'feature/1')
                        return sandbox
                    }
                }))
                .newGitComponent('c2',
                new SandboxClosure(new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        def ant = new AntBuilder()
                        ant.copy(todir: dir) {
                            fileset(dir: 'src/test/resources/c2') {
                                include(name: '**/**')
                            }
                        }
                        Git.add(sandbox.context, dir, '*.*')
                        Git.commit(sandbox.context, dir, 'add')
                        Git.createBranch(sandbox.context, dir, 'feature/1')
                        return sandbox
                    }
                }))
                .newGitComponent('c3',
                new SandboxClosure(new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        def ant = new AntBuilder()
                        ant.copy(todir: dir) {
                            fileset(dir: 'src/test/resources/c3') {
                                include(name: '**/**')
                            }
                        }
                        Git.add(sandbox.context, dir, '*.*')
                        Git.commit(sandbox.context, dir, 'add')
                        Git.createBranch(sandbox.context, dir, 'feature/1')
                        return sandbox
                    }
                }))
                .newGitComponent('c4',
                new SandboxClosure(new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        def ant = new AntBuilder()
                        ant.copy(todir: dir) {
                            fileset(dir: 'src/test/resources/c4') {
                                include(name: '**/**')
                            }
                        }
                        Git.add(sandbox.context, dir, '*.*')
                        Git.commit(sandbox.context, dir, 'add')
                        Git.createBranch(sandbox.context, dir, 'feature/1')
                        return sandbox
                    }
                }))
                .newGitComponent('manifest',
                new SandboxClosure(new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        sandbox.gitInitialCommit(dir)
                        sandbox.buildManifest(dir)
                        Git.add(sandbox.context, dir, 'default.xml')
                        Git.commit(sandbox.context, dir, 'manifest')
                        return sandbox
                    }
                }))
        MavenFeature.purgeLocal(sandbox.context,
                'test.repo-build'
        )

    }

    @Test
    void testUpdateReleaseParent() {

        def url = new File(sandbox.env.basedir, 'manifest')
        GitFeature.cloneManifest(context, url.getAbsolutePath(), 'master')

        // install parent 1.0.0-SNAPSHOT
        cleanInstallParent()

        updateInstallParent('1.1.0')
        updateInstallParent('1.1.1')
        updateInstallParent('1.1.2-SNAPSHOT')

        GitFeature.sync(context)

        MavenFeature.updateReleaseParent(context, 'parent', false, false)

        // check parent version
        def c1Pom = new XmlParser().parse(new File(env.basedir, 'c1/pom.xml'))
        assertEquals('1.1.1', c1Pom.parent.version.text())
        def c2Pom = new XmlParser().parse(new File(env.basedir, 'c2/pom.xml'))
        assertEquals('1.1.1', c2Pom.parent.version.text())
        def c3Pom = new XmlParser().parse(new File(env.basedir, 'c3/pom.xml'))
        assertEquals('1.1.1', c3Pom.parent.version.text())
    }

    @Test
    void testUpdateFeatureParent() {
        def url = new File(sandbox.env.basedir, 'manifest')
        GitFeature.cloneManifest(context, url.getAbsolutePath(), 'master')

        // execute parent
        cleanInstallParent()

        updateInstallParent('1.1.0-SNAPSHOT')
        updateInstallParent('1.1.1-SNAPSHOT')


        GitFeature.sync(context)
        GitFeature.switch(context, 'feature/1')
        GitFeature.featureMergeRelease(context, 'feature/1')

        MavenFeature.updateFeatureParent(context, 'feature/1', 'parent', true, true)

        // check parent version
        def c1Pom = new XmlParser().parse(new File(env.basedir, 'c1/pom.xml'))
        assertEquals('1.1.1-SNAPSHOT', c1Pom.parent.version.text())
        def c2Pom = new XmlParser().parse(new File(env.basedir, 'c2/pom.xml'))
        assertEquals('1.1.1-SNAPSHOT', c2Pom.parent.version.text())
        def c3Pom = new XmlParser().parse(new File(env.basedir, 'c3/pom.xml'))
        assertEquals('1.1.1-SNAPSHOT', c3Pom.parent.version.text())
    }

    def updateInstallParent(String version) {
        sandbox.component("parent", new SandboxClosure(
                new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        Maven.execute(sandbox.context, new File(dir, 'pom.xml'),
                                new MavenRequest(
                                        new Function1<InvocationRequest, Unit>() {
                                            @Override
                                            Unit invoke(InvocationRequest req) {
                                                req.setGoals(Arrays.asList("versions:set"))
                                                req.setInteractive(false)
                                                Properties properties = new Properties()
                                                properties.put("newVersion", version)
                                                properties.put('generateBackupPoms', 'false')
                                                req.setProperties(properties)
                                                return Unit.INSTANCE
                                            }
                                        }
                                )
                        )
                        Git.add(sandbox.context, dir, 'pom.xml')
                        Git.commit(sandbox.context, dir, 'vup')
                        return sandbox
                    }
                }
        ))

        cleanInstallParent()
    }

    @Test
    void testUpdateVersions() {
        def url = new File(sandbox.env.basedir, 'manifest')
        GitFeature.cloneManifest(context, url.getAbsolutePath(), 'master')

        // execute parent
        cleanInstallParent()
        // update c1 version to 1.1.0-SNAPSHOT on master
        sandbox.component("c1", new SandboxClosure(
                new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        Maven.execute(sandbox.context, new File(dir, 'pom.xml'),
                                new MavenRequest(
                                        new Function1<InvocationRequest, Unit>() {
                                            @Override
                                            Unit invoke(InvocationRequest req) {
                                                req.setGoals(Arrays.asList("versions:set"))
                                                req.setInteractive(false)
                                                Properties properties = new Properties()
                                                properties.put("newVersion", '1.1.0-SNAPSHOT')
                                                properties.put('generateBackupPoms', 'false')
                                                req.setProperties(properties)
                                                return Unit.INSTANCE
                                            }
                                        }
                                )
                        )
                        Git.addUpdated(sandbox.context, dir)
                        Git.commit(sandbox.context, dir, 'vup')
                        return sandbox
                    }
                }
        ))
        // update c2 version to 2.1.0-SNAPSHOT on master
        sandbox.component("c2", new SandboxClosure(
                new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        Maven.execute(sandbox.context, new File(dir, 'pom.xml'),
                                new MavenRequest(
                                        new Function1<InvocationRequest, Unit>() {
                                            @Override
                                            Unit invoke(InvocationRequest req) {
                                                req.setGoals(Arrays.asList("versions:set"))
                                                req.setInteractive(false)
                                                Properties properties = new Properties()
                                                properties.put("newVersion", '2.1.0-SNAPSHOT')
                                                properties.put('generateBackupPoms', 'false')
                                                req.setProperties(properties)
                                                return Unit.INSTANCE
                                            }
                                        }
                                )
                        )
                        Git.addUpdated(sandbox.context, dir)
                        Git.commit(sandbox.context, dir, 'vup')
                        return sandbox
                    }
                }
        ))

        GitFeature.sync(context)
        GitFeature.switch(context, 'feature/1')
        GitFeature.featureMergeRelease(context, 'feature/1')

        MavenFeature.buildParents(context)

        MavenFeature.updateVersions(context, 'feature/1', 'test.repo-build:*', null, true)

        // check parent version
        def c2Pom = new XmlParser().parse(new File(env.basedir, 'c2/pom.xml'))
        assertEquals('1.1.0-SNAPSHOT', c2Pom.properties."c1.version".text())
    }

    @Test
    void testUpdateVersionsContinueFromComponent() {
        def url = new File(sandbox.env.basedir, 'manifest')
        GitFeature.cloneManifest(context, url.getAbsolutePath(), 'master')

        // execute parent
        cleanInstallParent()
        // update c1 version to 1.1.0-SNAPSHOT on master
        sandbox.component("c1", new SandboxClosure(
                new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        Maven.execute(sandbox.context, new File(dir, 'pom.xml'),
                                new MavenRequest(
                                        new Function1<InvocationRequest, Unit>() {
                                            @Override
                                            Unit invoke(InvocationRequest req) {
                                                req.setGoals(Arrays.asList("versions:set"))
                                                req.setInteractive(false)
                                                Properties properties = new Properties()
                                                properties.put("newVersion", '1.1.0-SNAPSHOT')
                                                properties.put('generateBackupPoms', 'false')
                                                req.setProperties(properties)
                                                return Unit.INSTANCE
                                            }
                                        }
                                )
                        )
                        Git.addUpdated(sandbox.context, dir)
                        Git.commit(sandbox.context, dir, 'vup')
                        return sandbox
                    }
                }
        ))
        // update c2 version to 2.1.0-SNAPSHOT on master
        sandbox.component("c2", new SandboxClosure(
                new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        Maven.execute(sandbox.context, new File(dir, 'pom.xml'),
                                new MavenRequest(
                                        new Function1<InvocationRequest, Unit>() {
                                            @Override
                                            Unit invoke(InvocationRequest req) {
                                                req.setGoals(Arrays.asList("versions:set"))
                                                req.setInteractive(false)
                                                Properties properties = new Properties()
                                                properties.put("newVersion", '2.1.0-SNAPSHOT')
                                                properties.put('generateBackupPoms', 'false')
                                                req.setProperties(properties)
                                                return Unit.INSTANCE
                                            }
                                        }
                                )
                        )
                        Git.addUpdated(sandbox.context, dir)
                        Git.commit(sandbox.context, dir, 'vup')
                        return sandbox
                    }
                }
        ))

        GitFeature.sync(context)
        GitFeature.switch(context, 'feature/1')
        GitFeature.featureMergeRelease(context, 'feature/1')

        MavenFeature.buildParents(context)

        MavenFeature.updateVersions(context, 'feature/1', 'test.repo-build:*', null, true)

        sandbox.component("c1", new SandboxClosure(
                new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        Maven.execute(sandbox.context, new File(dir, 'pom.xml'),
                                new MavenRequest(
                                        new Function1<InvocationRequest, Unit>() {
                                            @Override
                                            Unit invoke(InvocationRequest req) {
                                                req.setGoals(Arrays.asList("clean"))
                                                req.setInteractive(false)
                                                return Unit.INSTANCE
                                            }
                                        }
                                )
                        )
                        return sandbox
                    }
                }
        ))

        MavenFeature.updateVersions(context, 'feature/1', 'test.repo-execute:*', 'c2', true)

        // check parent version
        def c2Pom = new XmlParser().parse(new File(env.basedir, 'c2/pom.xml'))
        assertEquals('1.1.0-SNAPSHOT', c2Pom.properties."c1.version".text())

        def c1Target = new File(env.basedir, 'c1/target')
        assertFalse(c1Target.exists())

    }

    private Sandbox cleanInstallParent() {

        sandbox.component("parent", new SandboxClosure(
                new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        Maven.execute(sandbox.context, new File(dir, 'pom.xml'),
                                new MavenRequest(
                                        new Function1<InvocationRequest, Unit>() {
                                            @Override
                                            Unit invoke(InvocationRequest req) {
                                                req.setGoals(Arrays.asList('clean', 'install'))
                                                req.setInteractive(false)
                                                Properties properties = new Properties()
                                                properties.put('skipTests', 'true')
                                                req.setProperties(properties)
                                                return Unit.INSTANCE
                                            }
                                        }
                                )
                        )
                        return sandbox
                    }
                }
        ))
    }

    @Test
    void testGetComponentsMap() {
        def url = new File(sandbox.env.basedir, 'manifest')
        GitFeature.cloneManifest(context, url.getAbsolutePath(), 'master')
        GitFeature.sync(context)
        GitFeature.switch(context, 'feature/1')
        Pom.generateXml(context, 'feature/1', new File(env.basedir, 'pom.xml'))

        def componentsMap = ComponentDependencyGraph.getModuleToComponentMap(MavenFeature.getComponents(context))
        assertEquals(10, componentsMap.size())
    }

    @Test
    void testSortComponents() {
        def url = new File(sandbox.env.basedir, 'manifest')
        GitFeature.cloneManifest(context, url.getAbsolutePath(), 'master')
        GitFeature.sync(context)
        GitFeature.switch(context, 'feature/1')
        Pom.generateXml(context, 'feature/1', new File(env.basedir, 'pom.xml'))

        def components = MavenFeature.getComponents(context)
        def sortedComponents = MavenFeature.sortComponents(components)
        assertEquals(6, sortedComponents.size())
        assertEquals('parent', sortedComponents.get(0).getArtifactId())
        assertEquals('c1-parent', sortedComponents.get(1).getArtifactId())
        assertEquals('c2-parent', sortedComponents.get(2).getArtifactId())
        assertEquals('parent2', sortedComponents.get(3).getArtifactId())
        assertEquals('c4-parent', sortedComponents.get(4).getArtifactId())
        assertEquals('c3-parent', sortedComponents.get(5).getArtifactId())
    }

    @Test
    void testSortParentComponents() {
        def url = new File(sandbox.env.basedir, 'manifest')
        GitFeature.cloneManifest(context, url.getAbsolutePath(), 'master')
        GitFeature.sync(context)
        GitFeature.switch(context, 'feature/1')
        Pom.generateXml(context, 'feature/1', new File(env.basedir, 'pom.xml'))

        def components = MavenFeature.getParentComponents(MavenFeature.getComponents(context))

        def sortedComponents = MavenFeature.sortComponents(components)
        assertEquals(2, sortedComponents.size())
        assertEquals('parent', sortedComponents.get(0).getArtifactId())
        assertEquals('parent2', sortedComponents.get(1).getArtifactId())
    }


    @Test
    void testBuildParents() {
        def url = new File(sandbox.env.basedir, 'manifest')
        GitFeature.cloneManifest(context, url.getAbsolutePath(), 'master')
        GitFeature.sync(context)
        GitFeature.switch(context, 'feature/1')
        Pom.generateXml(context, 'feature/1', new File(env.basedir, 'pom.xml'))

        MavenFeature.buildParents(context)
        Maven.execute(context, new File(env.basedir, 'pom.xml'),
                ['clean', 'install'],
                new Properties() as Map<String, String>)
    }


    @Test
    void testBuildParallel() {
        def url = new File(sandbox.env.basedir, 'manifest')
        GitFeature.cloneManifest(context, url.getAbsolutePath(), 'master')
        GitFeature.sync(context)
        GitFeature.switch(context, 'feature/1')

        assertTrue(MavenFeature.buildParallel(context))
    }

    @Test
    void testBuildParallelFail() {
        def url = new File(sandbox.env.basedir, 'manifest')
        GitFeature.cloneManifest(context, url.getAbsolutePath(), 'master')
        GitFeature.sync(context)
        GitFeature.switch(context, 'feature/1')

        // create class with syntax error
        new File(context.env.basedir, 'c2/api/src/main/java/Test.java').text = 'blablabla class'

        assertFalse(MavenFeature.buildParallel(context))
    }

    @Test
    void testBuildParallelCircularDepsFail() {
        def url = new File(sandbox.env.basedir, 'manifest')

        sandbox.component("c1", new SandboxClosure(
                new Function2<Sandbox, File, Sandbox>() {
                    @Override
                    Sandbox invoke(Sandbox sandbox, File dir) {
                        Git.checkout(sandbox.context, dir, "feature/1")
                        def ant = new AntBuilder()
                        ant.copy(todir: dir, overwrite: true) {
                            fileset(dir: 'src/test/resources/circular/c1') {
                                include(name: '**/**')
                            }
                        }
                        Git.add(sandbox.context, dir, '*.*')
                        Git.commit(sandbox.context, dir, 'add')
                        return sandbox
                    }
                }
        ))

        GitFeature.cloneManifest(context, url.getAbsolutePath(), 'master')
        GitFeature.sync(context)
        GitFeature.switch(context, 'feature/1')

        try {
            MavenFeature.buildParallel(context)
            fail()
        } catch (RepoBuildException e) {

        }
    }
}