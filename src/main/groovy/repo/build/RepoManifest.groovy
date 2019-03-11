package repo.build

import groovy.transform.CompileStatic
import groovyx.gpars.GParsPool
import kotlin.Unit
import kotlin.jvm.functions.Function2
import org.apache.log4j.Logger

class RepoManifest {
    static Logger logger = Logger.getLogger(RepoManifest.class)

    static String getRemoteName(ActionContext context) {
        return context.env.manifest.remote[0].@name
    }

    static String getRemoteBaseUrl(ActionContext context) {
        if (context.options.getManifestRemote()) {
            return context.options.getManifestRemote()
        } else {
            return context.env.manifest.remote[0].@fetch
        }
    }

    static boolean projectDirExists(ActionContext context, project) {
        def dir = new File(context.env.basedir, project.@path)
        return dir.exists()
    }

    @CompileStatic
    static String getRemoteBranch(ActionContext context, String branch) {
        def remoteName = getRemoteName(context)
        return "$remoteName/$branch"
    }

    static void forEach(ActionContext parentContext, ManifestFilter filter, ManifestAction action) {
        forEach(parentContext, filter, action,
                new ManifestLogHeader(new Function2<ActionContext, Node, Unit>() {
                    @Override
                    Unit invoke(ActionContext actionContext, Node project) {
                        def path = project.@path
                        actionContext.newChildWriteOut("$path\n")
                        return null
                    }
                }),
                new ManifestLogFooter(new Function2<ActionContext, Node, Unit>() {
                    @Override
                    Unit invoke(ActionContext actionContext, Node project) {
                        actionContext.newChildWriteOut("\n")
                        return null
                    }
                })

        )
    }

    public final static String ACTION_FOR_EACH = 'repoManifestForEach'
    public final static String ACTION_FOR_EACH_ITERATION = 'repoManifestForEachIteraction'

    static void forEach(ActionContext parentContext,
                        ManifestFilter filter,
                        ManifestAction action,
                        ManifestLogHeader logHeader,
                        ManifestLogFooter logFooter) {
        def context = parentContext.newChild(ACTION_FOR_EACH)
        context.withCloseable {
            GParsPool.withPool(context.getParallel(), {
                parentContext.env.manifest.project
                        .eachParallel { project ->
                    def actionContext = context.newChild(ACTION_FOR_EACH_ITERATION)
                    actionContext.withCloseable {
                        try {
                            if (filter.invoke(actionContext, project)) {
                                if (logHeader != null) {
                                    logHeader.invoke(actionContext, project)
                                }
                                action.invoke(actionContext, project)
                                if (logFooter != null) {
                                    logFooter.invoke(actionContext, project)
                                }
                            }
                            return null
                        }
                        catch (Exception e) {
                            def componentError = new RepoBuildException("Component ${project.@path} error ${e.message}", e)
                            if (actionContext.options.hasFae()) {
                                actionContext.addError(componentError)
                            } else {
                                throw componentError
                            }
                        }
                    }
                }
            })
        }
    }


    @CompileStatic
    static void forEach(ActionContext parentContext, ManifestAction action) {
        forEach(parentContext,
                new ManifestFilter(new Function2<ActionContext, Node, Boolean>() {
                    @Override
                    Boolean invoke(ActionContext actionContext, Node Node) {
                        return true
                    }
                }),
                action)
    }

    static void forEachWithBranch(ActionContext parentContext, ManifestAction action, String branch) {
        def remoteBranch = getRemoteBranch(parentContext, branch)

        forEach(parentContext,
                new ManifestFilter(new Function2<ActionContext, Node, Boolean>() {
                    @Override
                    Boolean invoke(ActionContext actionContext, Node project) {
                        return Git.branchPresent(actionContext, new File(actionContext.env.basedir, project.@path), remoteBranch)
                    }
                }),
                action
        )
    }

    static String getBranch(ActionContext context, String projectPath) {
        return context.env.manifest.project
                .findAll {
            (projectPath == it.@path)
        }
        .first()
                .@revision
                .replaceFirst("refs/heads/", "")
    }

}
