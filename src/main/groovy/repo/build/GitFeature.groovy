package repo.build

import com.github.zafarkhaja.semver.UnexpectedCharacterException
import com.github.zafarkhaja.semver.Version
import groovy.xml.XmlUtil
import kotlin.Unit
import kotlin.jvm.functions.Function2

class GitFeature {

    static File getManifestDir(ActionContext context) {
        return new File(context.env.basedir, 'manifest')
    }

    public static final String ACTION_CLONE_MANIFEST = 'gitFeatureCloneManifest'

    static void cloneManifest(ActionContext parentContext, String url, String branch) {
        def context = parentContext.newChild(ACTION_CLONE_MANIFEST)
        context.withCloseable {
            def dir = getManifestDir(context)
            dir.mkdirs()
            Git.clone(context, url, "origin", dir)
            Git.checkoutUpdate(context, branch, "origin/$branch", dir)
        }
    }

    public static final String ACTION_UPDATE_MANIFEST = 'gitFeatureUpdateManifest'

    static void updateManifest(ActionContext parentContext, String branch) {
        def context = parentContext.newChild(ACTION_UPDATE_MANIFEST)
        context.withCloseable {
            def dir = getManifestDir(context)
            Git.fetch(context, "origin", dir)
            Git.checkoutUpdate(context, branch, "origin/$branch", dir)
        }
    }

    public static final String ACTION_SYNC = 'gitFeatureSync'

    static void sync(ActionContext parentContext) {
        def context = parentContext.newChild(ACTION_SYNC)
        context.withCloseable {
            def manifestDir = getManifestDir(context)
            if (manifestDir.exists()) {
                def manifestBranch = Git.getBranch(context, manifestDir)
                if ("HEAD" == manifestBranch) {
                    throw new RepoBuildException("manifest branch must be local, use: repo-execute -b <manifestBranch> init")
                }
                updateManifest(context, manifestBranch)
                context.env.openManifest()
                fetchUpdate(context)
            } else {
                throw new RepoBuildException("manifest dir $manifestDir not found")
            }
        }
    }

    static void featureMergeRelease(ActionContext parentContext, String featureBranch) {
        def context = parentContext.newChild()
        context.withCloseable {
/*            RepoManifest.forEachWithBranch(context,
                    { ActionContext actionContext, project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        if (Git.getBranch(actionContext, dir) != featureBranch) {
                            throw new RepoBuildException("must be set to branch $featureBranch")
                        }
                        def manifestBranch = RepoManifest.getBranch(actionContext, project.@path)
                        Git.merge(actionContext, manifestBranch, dir)
                    }, featureBranch)*/
            RepoManifest.forEachWithBranch(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            if (Git.getBranch(actionContext, dir) != featureBranch) {
                                throw new RepoBuildException("must be set to branch $featureBranch")
                            }
                            def manifestBranch = RepoManifest.getBranch(actionContext, project.@path)
                            Git.merge(actionContext, manifestBranch, dir)
                        }
                    }), featureBranch)
        }
    }

    static void releaseMergeFeature(ActionContext parentContext, String featureBranch) {
        def context = parentContext.newChild()
        context.withCloseable {
/*            RepoManifest.forEachWithBranch(context,
                    { ActionContext actionContext, project ->
                        // check current components branch
                        def dir = new File(actionContext.env.basedir, project.@path)
                        def manifestBranch = RepoManifest.getBranch(actionContext, project.@path)
                        if (Git.getBranch(actionContext, dir) != manifestBranch) {
                            throw new RepoBuildException("must be set to branch $manifestBranch")
                        }
                        Git.merge(actionContext, featureBranch, dir)
                    }, featureBranch)*/
            RepoManifest.forEachWithBranch(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            // check current components branch
                            def dir = new File(actionContext.env.basedir, project.@path)
                            def manifestBranch = RepoManifest.getBranch(actionContext, project.@path)
                            if (Git.getBranch(actionContext, dir) != manifestBranch) {
                                throw new RepoBuildException("must be set to branch $manifestBranch")
                            }
                            Git.merge(actionContext, featureBranch, dir)
                        }
                    }), featureBranch)
        }
    }

    static void releaseMergeRelease(ActionContext context,
                                    String sourceRelease,
                                    String destinationRelease,
                                    String regexp) {
        updateManifest(context, sourceRelease)
        context.env.openManifest()

        Map<String, String> sourceBranches = new HashMap<>()
/*        RepoManifest.forEach(context, { ActionContext actionContext, Node project ->
            sourceBranches.put(project.attribute("name").toString(),
                    project.attribute("revision").toString().replace("refs/heads/", ""))
        })*/
        RepoManifest.forEach(context,
                new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                    @Override
                    Unit invoke(ActionContext actionContext, Node project) {
                        sourceBranches.put(project.attribute("name").toString(),
                                project.attribute("revision").toString().replace("refs/heads/", ""))
                        return null
                    }
                })
        )

        updateManifest(context, destinationRelease)
        sync(context)

/*        RepoManifest.forEach(context, { ActionContext actionContext, Node project ->
            def component = project.attribute("name")
            def dir = new File(context.env.basedir, project.@path)
            def targetBranch = project.attribute("revision").toString().replace("refs/heads/", "")
            def sourceBranch = sourceBranches.get(component)
            if (sourceBranch != null) {
                try {
                    Version targetVersion = Version.valueOf(targetBranch.find(regexp, versionClosure).toString())
                    Version sourceVersion = Version.valueOf(sourceBranch.find(regexp, versionClosure).toString())

                    if (targetVersion.greaterThanOrEqualTo(sourceVersion)) {
                        Git.merge(context, RepoManifest.getRemoteBranch(context, sourceBranch), dir)
                    } else {
                        actionContext.addError(new RepoBuildException("Сomponent $component wasn't automatic merge because the current version $targetBranch is younger $sourceBranch"))
                    }
                } catch (UnexpectedCharacterException | IllegalArgumentException | NullPointerException e) {
                    actionContext.addError(new RepoBuildException("Cannot be automatic merge component $component version $sourceBranch to $targetBranch", e))
                }
            }
        })*/
        RepoManifest.forEach(context,
                new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                    @Override
                    Unit invoke(ActionContext actionContext, Node project) {
                        def component = project.attribute("name")
                        def dir = new File(context.env.basedir, project.@path)
                        def targetBranch = project.attribute("revision").toString().replace("refs/heads/", "")
                        def sourceBranch = sourceBranches.get(component)
                        if (sourceBranch != null) {
                            try {
                                def targetStringVersion = targetBranch.find(regexp).toString()
                                Version targetVersion
                                if (targetStringVersion.split("\\.").length < 3) {
                                    targetVersion = Version.valueOf(targetStringVersion += ".0")
                                } else {
                                    targetVersion = Version.valueOf(targetStringVersion)
                                }
                                def sourceStringVersion = sourceBranch.find(regexp).toString()
                                Version sourceVersion
                                if (sourceStringVersion.split("\\.").length < 3) {
                                    sourceVersion = Version.valueOf(sourceStringVersion += ".0")
                                } else {
                                    sourceVersion = Version.valueOf(sourceStringVersion)
                                }
                                if (targetVersion.greaterThanOrEqualTo(sourceVersion)) {
                                    Git.merge(context, RepoManifest.getRemoteBranch(context, sourceBranch), dir)
                                } else {
                                    actionContext.addError(new RepoBuildException("Сomponent $component wasn't automatic merge because the current version $targetBranch is younger $sourceBranch"))
                                    return null
                                }
                            } catch (UnexpectedCharacterException | IllegalArgumentException | NullPointerException e) {
                                actionContext.addError(new RepoBuildException("Cannot be automatic merge component $component version $sourceBranch to $targetBranch", e))
                                return null
                            }
                        }
                    }
                })
        )
    }

    static void taskMergeFeature(ActionContext parentContext, String taskBranch, String featureBranch) {
        def context = parentContext.newChild()
        context.withCloseable {
/*            RepoManifest.forEachWithBranch(context,
                    { ActionContext actionContext, project ->
                        // check current components branch
                        def dir = new File(actionContext.env.basedir, project.@path)
                        if (Git.branchPresent(actionContext, dir, taskBranch)) {
                            if (Git.getBranch(actionContext, dir) != taskBranch) {
                                throw new RepoBuildException("must be set to branch $taskBranch")
                            }
                            Git.merge(actionContext, featureBranch, dir)
                        }
                    }, featureBranch)*/
            RepoManifest.forEachWithBranch(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            // check current components branch
                            def dir = new File(actionContext.env.basedir, project.@path)
                            if (Git.branchPresent(actionContext, dir, taskBranch)) {
                                if (Git.getBranch(actionContext, dir) != taskBranch) {
                                    throw new RepoBuildException("must be set to branch $taskBranch")
                                }
                                Git.merge(actionContext, featureBranch, dir)
                            }
                        }
                    }), featureBranch)
        }
    }

    static void branchMergeFeature(ActionContext parentContext, String branch, String featureBranch) {
        def context = parentContext.newChild()
        context.withCloseable {
/*            RepoManifest.forEachWithBranch(context,
                    { ActionContext actionContext, project ->
                        // check current components branch
                        def dir = new File(actionContext.env.basedir, project.@path)
                        if (Git.branchPresent(actionContext, dir, branch)) {
                            def remoteBranch = RepoManifest.getRemoteBranch(actionContext, branch)
                            Git.checkoutUpdate(actionContext, branch, remoteBranch, dir)
                            Git.merge(actionContext, featureBranch, dir)
                        }
                    }, branch)*/
            RepoManifest.forEachWithBranch(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            // check current components branch
                            def dir = new File(actionContext.env.basedir, project.@path)
                            if (Git.branchPresent(actionContext, dir, branch)) {
                                def remoteBranch = RepoManifest.getRemoteBranch(actionContext, branch)
                                Git.checkoutUpdate(actionContext, branch, remoteBranch, dir)
                                Git.merge(actionContext, featureBranch, dir)
                            }
                        }
                    })
                    , featureBranch
            )
        }
    }

    static void 'switch'(ActionContext parentContext, String featureBranch) {
        'switch'(parentContext, featureBranch, null)
    }

    static void 'switch'(ActionContext parentContext, String featureBranch, String taskBranch) {
        def context = parentContext.newChild()
        context.withCloseable {
/*            RepoManifest.forEach(context,
                    { ActionContext actionContext, Node project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        // switch if feature the branch exists
                        if (checkoutUpdateIfExists(actionContext, dir, featureBranch)) {
                            // switch task if the branch exists
                            if (taskBranch != null) {
                                checkoutUpdateIfExists(actionContext, dir, taskBranch)
                            }
                        } else if (Git.branchPresent(context, dir, taskBranch)) {
                            throw new RepoBuildException("task $taskBranch exists but $featureBranch not exists")
                        } else {
                            // switch to manifest branch
                            def manifestBranch = RepoManifest.getBranch(actionContext, project.@path)
                            def remoteManifestBranch = RepoManifest.getRemoteBranch(context, manifestBranch)
                            Git.checkoutUpdate(context, manifestBranch, remoteManifestBranch, dir)
                        }
                    }
            )*/
            RepoManifest.forEach(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            // switch if feature the branch exists
                            if (checkoutUpdateIfExists(actionContext, dir, featureBranch)) {
                                // switch task if the branch exists
                                if (taskBranch != null) {
                                    checkoutUpdateIfExists(actionContext, dir, taskBranch)
                                    return null
                                }
                            } else if (Git.branchPresent(context, dir, taskBranch)) {
                                throw new RepoBuildException("task $taskBranch exists but $featureBranch not exists")
                            } else {
                                // switch to manifest branch
                                def manifestBranch = RepoManifest.getBranch(actionContext, project.@path)
                                def remoteManifestBranch = RepoManifest.getRemoteBranch(context, manifestBranch)
                                Git.checkoutUpdate(context, manifestBranch, remoteManifestBranch, dir)
                            }
                        }
                    }))
        }
    }

    private static boolean checkoutUpdateIfExists(ActionContext context, File dir, String branch) {
        def remoteBranch = RepoManifest.getRemoteBranch(context, branch)
        if (Git.branchPresent(context, dir, branch)
                || Git.branchPresent(context, dir, remoteBranch)) {
            Git.checkoutUpdate(context, branch, remoteBranch, dir)
            return true
        } else {
            return false
        }
    }

    public static final String ACTION_FETCH_UPDATE = 'gitFeatureFetchUpdate'

    static void fetchUpdate(ActionContext parentContext) {
        def context = parentContext.newChild(ACTION_FETCH_UPDATE)
        context.withCloseable {
            def remoteName = RepoManifest.getRemoteName(context)
            def remoteBaseUrl = RepoManifest.getRemoteBaseUrl(context)
            RepoManifest.forEach(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def branch = RepoManifest.getBranch(actionContext, project.@path)
                            def env = actionContext.env
                            def remoteBranch = RepoManifest.getRemoteBranch(actionContext, branch)
                            def dir = new File(env.basedir, project.@path)
                            def name = project.@name
                            if (new File(dir, ".git").exists()) {
                                Git.fetch(actionContext, remoteName, dir)
                            } else {
                                dir.mkdirs()
                                Git.clone(actionContext, "$remoteBaseUrl/$name", remoteName, dir)
                            }
                            Git.checkoutUpdate(actionContext, branch, remoteBranch, dir)
                            Git.user(actionContext, dir,
                                    env.props.getProperty("git.user.name"),
                                    env.props.getProperty("git.user.email"))
                            return null
                        }
                    })
            )
        }
    }

    static void releaseMergeFeature(ActionContext parentContext, String branch, Boolean mergeAbort) {
        def context = parentContext.newChild()
        context.withCloseable {
            def remoteBranch = RepoManifest.getRemoteBranch(context, branch)
/*            RepoManifest.forEachWithBranch(context,
                    { ActionContext actionContext, Node project ->
                        def env = actionContext.env
                        def dir = new File(env.basedir, project.@path)
                        def manifestBranch = RepoManifest.getBranch(actionContext, project.@path)
                        if (Git.getBranch(actionContext, dir) != manifestBranch) {
                            throw new RepoBuildException("must be set to branch $manifestBranch")
                        }
                        def startCommit = project.@revision.replaceFirst("refs/heads", env.manifest.remote[0].@name)
                        if (mergeAbort) {
                            try {
                                Git.mergeAbort(context, dir)
                            } catch (Exception ignored) {
                            }
                        }
                        Git.mergeFeatureBranch(actionContext, branch, remoteBranch, startCommit, dir)
                    },
                    branch
            )*/
            RepoManifest.forEachWithBranch(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def env = actionContext.env
                            def dir = new File(env.basedir, project.@path)
                            def manifestBranch = RepoManifest.getBranch(actionContext, project.@path)
                            if (Git.getBranch(actionContext, dir) != manifestBranch) {
                                throw new RepoBuildException("must be set to branch $manifestBranch")
                            }
                            def startCommit = project.@revision.replaceFirst("refs/heads", env.manifest.remote[0].@name)
                            if (mergeAbort) {
                                try {
                                    Git.mergeAbort(context, dir)
                                } catch (Exception ignored) {
                                }
                            }
                            Git.mergeFeatureBranch(actionContext, branch, remoteBranch, startCommit, dir)
                            return null
                        }
                    }),
                    branch)
        }
    }

    public static final String ACTION_CREATE_FEATURE_BUNDLES = 'gitFeatureCreateFeatureBundles'

    static void createFeatureBundles(ActionContext parentContext, File targetDir, String branch, Map<String, String> commits = null) {
        def context = parentContext.newChild(ACTION_CREATE_FEATURE_BUNDLES)
        context.withCloseable {
            if (commits == null) {
                commits = [:]
            }

/*            RepoManifest.forEachWithBranch(context,
                    { ActionContext actionContext, Node project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        //println gitName
                        def bundleFile = new File(targetDir, project.@name)
                        Git.createFeatureBundle(actionContext, branch, dir, bundleFile, commits.get(project.@name))
                    },
                    branch
            )*/
            RepoManifest.forEachWithBranch(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            //println gitName
                            def bundleFile = new File(targetDir, project.@name)
                            Git.createFeatureBundle(actionContext, branch, dir, bundleFile, commits.get(project.@name))
                        }
                    })
                    , branch)
        }
    }

    public static final String ACTION_CREATE_MANIFEST_BUNDLES = 'gitCreateManifestBundles'

    static void createManifestBundles(ActionContext parentContext, File targetDir, Map<String, String> commits = null) {
        def context = parentContext.newChild(ACTION_CREATE_MANIFEST_BUNDLES)
        context.withCloseable {
            if (commits == null) {
                commits = [:]
            }
/*            RepoManifest.forEach(context,
                    { ActionContext actionContext, Node project ->
                        def branch = RepoManifest.getBranch(actionContext, project.@path)
                        def dir = new File(actionContext.env.basedir, project.@path)
                        //println gitName
                        def bundleFile = new File(targetDir, project.@name)
                        Git.createFeatureBundle(actionContext, branch, dir, bundleFile, commits.get(project.@name))
                    }
            )*/
            RepoManifest.forEach(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def branch = RepoManifest.getBranch(actionContext, project.@path)
                            def dir = new File(actionContext.env.basedir, project.@path)
                            //println gitName
                            def bundleFile = new File(targetDir, project.@name)
                            Git.createFeatureBundle(actionContext, branch, dir, bundleFile, commits.get(project.@name))
                            return null
                        }
                    })
            )
        }
    }

    public static final String ACTION_CREATE_BUNDLE_FOR_MANIFEST = 'gitCreateBundleForManifest'

    static void createBundleForManifest(ActionContext parentContext, File targetDir, String bundleName) {
        def context = parentContext.newChild(ACTION_CREATE_BUNDLE_FOR_MANIFEST)
        context.withCloseable {
            def manifestDir = new File(context.env.basedir, 'manifest')
            def bundleFile = new File(targetDir, bundleName)
            Git.createFeatureBundle(context, '--all', manifestDir, bundleFile, null)
        }
    }

    static void forEachWithProjectDirExists(ActionContext context, ManifestAction action) {
/*        RepoManifest.forEach(context,
                { ActionContext actionContext, Node project -> return RepoManifest.projectDirExists(actionContext, project) },
                action
        )*/
        RepoManifest.forEach(context,
                new ManifestFilter(new Function2<ActionContext, Node, Boolean>() {
                    @Override
                    Boolean invoke(ActionContext actionContext, Node project) {
                        return RepoManifest.projectDirExists(actionContext, project)
                    }
                }), action
        )
    }

    public static final String ACTION_STATUS = 'gitFeatureStatus'

    static void status(ActionContext parentContext) {
        def context = parentContext.newChild(ACTION_STATUS)
        context.withCloseable {
/*            forEachWithProjectDirExists(context,
                    { ActionContext actionContext, Node project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        def branch = Git.getBranch(actionContext, dir)
                        def remoteName = RepoManifest.getRemoteName(actionContext)
                        def remoteBranch = "$remoteName/$branch"
                        Git.status(actionContext, dir)
                        if (Git.branchPresent(actionContext, dir, remoteBranch)) {
                            Git.logUnpushed(actionContext, dir, remoteBranch)
                        } else {
                            Git.fetch(actionContext, remoteName, dir)
                            if (Git.branchPresent(actionContext, dir, remoteBranch)) {
                                Git.logUnpushed(actionContext, dir, remoteBranch)
                            } else {
                                def unpushed = "Branch not pushed"
                                actionContext.writeOut(unpushed + '\n')
                            }
                        }
                    }
            )*/
            forEachWithProjectDirExists(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            def branch = Git.getBranch(actionContext, dir)
                            def remoteName = RepoManifest.getRemoteName(actionContext)
                            def remoteBranch = "$remoteName/$branch"
                            Git.status(actionContext, dir)
                            if (Git.branchPresent(actionContext, dir, remoteBranch)) {
                                Git.logUnpushed(actionContext, dir, remoteBranch)
                                return null
                            } else {
                                Git.fetch(actionContext, remoteName, dir)
                                if (Git.branchPresent(actionContext, dir, remoteBranch)) {
                                    Git.logUnpushed(actionContext, dir, remoteBranch)
                                    return null
                                } else {
                                    def unpushed = "Branch not pushed"
                                    actionContext.writeOut(unpushed + '\n')
                                }
                            }
                        }
                    })
            )
        }
    }

    public static final String ACTION_GREP = 'gitFeatureGrep'

    static Map<String, String> grep(ActionContext parentContext, String exp) {
        Map<String, String> result = new HashMap<>()
        def context = parentContext.newChild(ACTION_GREP)
        context.withCloseable {
/*            forEachWithProjectDirExists(context,
                    { ActionContext actionContext, Node project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        def grepResult = Git.grep(actionContext, dir, exp)
                        synchronized (result) {
                            result.put(project.@path, grepResult)
                        }
                    }
            )*/
            forEachWithProjectDirExists(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            def grepResult = Git.grep(actionContext, dir, exp)
                            synchronized (result) {
                                result.put(project.@path, grepResult)
                            }
                        }
                    })
            )
        }
        return result
    }

    public static final String ACTION_MERGE_ABORT = 'gitFeatureMergeAbort'

    static void mergeAbort(ActionContext parentContext) {
        def context = parentContext.newChild(ACTION_MERGE_ABORT)
        context.withCloseable {
/*            forEachWithProjectDirExists(context,
                    { ActionContext actionContext, Node project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        Git.mergeAbort(actionContext, dir)
                    }
            )*/
            forEachWithProjectDirExists(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            Git.mergeAbort(actionContext, dir)
                        }
                    })
            )
        }
    }

    public static final String ACTION_STASH = 'gitFeatureStash'

    static void stash(ActionContext parentContext) {
        def context = parentContext.newChild(ACTION_STASH)
        context.withCloseable {
/*            forEachWithProjectDirExists(context,
                    { ActionContext actionContext, Node project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        Git.stash(actionContext, dir)
                    }
            )*/
            forEachWithProjectDirExists(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            Git.stash(actionContext, dir)
                        }
                    })
            )
        }
    }

    public static final String ACTION_STASH_POP = 'gitFeatureStashPop'

    static void stashPop(ActionContext parentContext) {
        def context = parentContext.newChild(ACTION_STASH_POP)
        context.withCloseable {
/*            forEachWithProjectDirExists(context,
                    { ActionContext actionContext, Node project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        Git.stashPop(actionContext, dir)
                    }
            )*/
            forEachWithProjectDirExists(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            Git.stashPop(actionContext, dir)
                        }
                    })
            )
        }
    }

    public static final String ACTION_PUSH_FEATURE_BRANCH = 'gitFeaturePushFeatureBranch'

    static void pushFeatureBranch(ActionContext parentContext, String featureBranch, boolean setUpstream) {
        def context = parentContext.newChild(ACTION_PUSH_FEATURE_BRANCH)
        context.withCloseable {
/*            RepoManifest.forEach(context,
                    // use only existing local componentn whith have featureBranch
                    { ActionContext actionContext, Node project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        return RepoManifest.projectDirExists(actionContext, project) && Git.branchPresent(actionContext, dir, featureBranch)
                    },
                    { ActionContext actionContext, Node project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        Git.pushBranch(actionContext, dir, project.@remote, featureBranch, setUpstream)
                    }
            )*/
            RepoManifest.forEach(context,
                    // use only existing local component which have featureBranch
                    new ManifestFilter(new Function2<ActionContext, Node, Boolean>() {
                        @Override
                        Boolean invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            return RepoManifest.projectDirExists(actionContext, project) && Git.branchPresent(actionContext, dir, featureBranch)
                        }
                    }),
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            Git.pushBranch(actionContext, dir, project.@remote, featureBranch, setUpstream)
                        }
                    })
            )
        }
    }

    public static final String ACTION_PUSH_MANIFEST_BRANCH = 'gitFeaturePushManifestBranch'

    static void pushManifestBranch(ActionContext parentContext, boolean setUpstream) {
        def context = parentContext.newChild(ACTION_PUSH_MANIFEST_BRANCH)
        context.withCloseable {
/*            forEachWithProjectDirExists(context,
                    { ActionContext actionContext, Node project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        def manifestBranch = RepoManifest.getBranch(actionContext, project.@path)
                        Git.pushBranch(actionContext, dir, project.@remote, manifestBranch, setUpstream)
                    }
            )*/
            forEachWithProjectDirExists(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            def manifestBranch = RepoManifest.getBranch(actionContext, project.@path)
                            Git.pushBranch(actionContext, dir, project.@remote, manifestBranch, setUpstream)
                        }
                    })
            )
        }
    }

    public static final String ACTION_ADD_TAG_TO_CURRENT_HEADS = 'gitFeatureAddTagToCurrentHeads'

    static void addTagToCurrentHeads(ActionContext parentContext, String tag) {
        def context = parentContext.newChild(ACTION_ADD_TAG_TO_CURRENT_HEADS)
        context.withCloseable {
/*            forEachWithProjectDirExists(context,
                    { ActionContext actionContext, Node project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        Git.addTagToCurrentHead(actionContext, dir, tag)
                    }
            )*/
            forEachWithProjectDirExists(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            Git.addTagToCurrentHead(actionContext, dir, tag)
                        }
                    })
            )
        }
    }

    public static final String ACTION_PUSH_TAG = 'gitFeaturePushTag'

    static void pushTag(ActionContext parentContext, String tag) {
        def context = parentContext.newChild(ACTION_PUSH_TAG)
        context.withCloseable {
/*            forEachWithProjectDirExists(context,
                    { ActionContext actionContext, Node project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        Git.pushTag(actionContext, dir, project.@remote, tag)
                    }
            )*/
            forEachWithProjectDirExists(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            Git.pushTag(actionContext, dir, project.@remote, tag)
                        }
                    })
            )
        }
    }

    public static final String ACTION_CHECKOUT_TAG = 'gitFeatureCheckoutTag'

    static void checkoutTag(ActionContext parentContext, String tag) {
        def context = parentContext.newChild(ACTION_CHECKOUT_TAG)
        context.withCloseable {
/*            RepoManifest.forEach(context,
                    // use only existing local componentn whith have tag
                    { ActionContext actionContext, Node project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        return RepoManifest.projectDirExists(actionContext, project) && Git.tagPresent(actionContext, dir, tag)
                    },
                    { ActionContext actionContext, Node project ->
                        def dir = new File(actionContext.env.basedir, project.@path)
                        Git.checkoutTag(actionContext, dir, tag)
                    }
            )*/
            RepoManifest.forEach(context,
                    new ManifestFilter(new Function2<ActionContext, Node, Boolean>() {
                        @Override
                        Boolean invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            return RepoManifest.projectDirExists(actionContext, project) && Git.tagPresent(actionContext, dir, tag)
                        }
                    }),
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def dir = new File(actionContext.env.basedir, project.@path)
                            Git.checkoutTag(actionContext, dir, tag)
                        }
                    })
            )
        }
    }

    public static final String ACTION_CLONE_OR_UPDATE_FROM_BUNDLES = 'gitCloneOrUpdateFromBundles'

    /**
     * Clone every .bundle file in specified directory, fetch branch specified in manifest
     * @param parentContext context
     * @param sourceImportDir bundle source directory
     */
    static void cloneOrUpdateFromBundles(ActionContext parentContext, File sourceImportDir) {
        def context = parentContext.newChild(ACTION_CLONE_OR_UPDATE_FROM_BUNDLES)
        context.withCloseable {
            context.env.openManifest()

/*            RepoManifest.forEach(context,
                    { ActionContext actionContext, Node project ->
                        def branch = RepoManifest.getBranch(actionContext, project.@path)

                        cloneOrUpdateFromBundle(context, sourceImportDir, project.@path, project.@name, branch)
                    }
            )*/

            RepoManifest.forEach(context,
                    new ManifestAction(new Function2<ActionContext, Node, Unit>() {
                        @Override
                        Unit invoke(ActionContext actionContext, Node project) {
                            def branch = RepoManifest.getBranch(actionContext, project.@path)
                            cloneOrUpdateFromBundle(context, sourceImportDir, project.@path, project.@name, branch)
                        }
                    })
            )
        }

    }

    public static final String ACTION_CLONE_FROM_BUNDLE = 'gitCloneManifestBundle'

    static cloneOrUpdateFromBundle(ActionContext parentContext, File sourceImportDir, String modulePath,
                                   String bundleName, String branch) {
        def context = parentContext.newChild(ACTION_CLONE_FROM_BUNDLE)
        context.withCloseable {
            def dir = new File(context.env.basedir, modulePath)
            def bundleFileName = new File(sourceImportDir, bundleName).getAbsolutePath()

            if (dir.exists()) {
                //change git remote if necessary
                def remotes = Git.getRemote(context, dir)
                def needChangeRemote = true
                remotes.each { line ->
                    def (name, url, type) = line.split(' ').collect { it.trim() }
                    if (url == sourceImportDir) {
                        needChangeRemote = false
                    }
                }
                if (needChangeRemote) {
                    Git.setRemote(context, dir, 'origin', bundleFileName)
                }

            } else {
                //clone from bundle
                Git.clone(context, bundleFileName, 'origin', dir)
            }

            if (Git.branchPresent(context, dir, branch)) {
                Git.checkout(context, dir, branch)
                Git.fetch(context, 'origin', dir, "$branch")
                Git.merge(context, 'FETCH_HEAD', dir)

            } else {
                try {
                    Git.fetch(context, 'origin', dir, "$branch:$branch")
                } catch (RepoBuildException e) {
                    Git.fetch(context, 'origin', dir, "$branch")
                    Git.merge(context, 'FETCH_HEAD', dir)
                }
                Git.checkout(context, dir, branch)
            }

        }
    }

    public static final String ACTION_LAST_COMMIT_BY_MANIFEST = 'gitLastCommitByManifest'

    static lastCommitByManifest(ActionContext parentContext) {
        def context = parentContext.newChild(ACTION_LAST_COMMIT_BY_MANIFEST)
        context.withCloseable {
            def lastCommits = [:]
            RepoManifest.forEach(context,
                    { ActionContext actionContext, Node project ->
                        lastCommits[project.@name] = Git.getLastCommit(context, new File(actionContext.env.getBasedir(), project.@path))
                    }
            )
            return lastCommits
        }
    }

}
