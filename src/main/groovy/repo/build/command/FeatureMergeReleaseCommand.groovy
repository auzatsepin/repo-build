package repo.build.command

import repo.build.*

class FeatureMergeReleaseCommand extends AbstractCommand {
    FeatureMergeReleaseCommand() {
        super('feature-merge-release', 'Merge current releease into feature branches')
    }

    public static final String ACTION_EXECUTE = 'featureMergeReleaseCommandExecute'

    void execute(RepoEnv env, CliOptions options) {
        def context = new ActionContext(env, ACTION_EXECUTE, options, new DefaultActionHandler())
        context.withCloseable {
            GitFeature.featureMergeRelease(context, options.getFeatureBranch())
        }
    }
}
