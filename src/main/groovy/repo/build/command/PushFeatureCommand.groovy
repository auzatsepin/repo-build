package repo.build.command

import groovy.transform.CompileStatic
import repo.build.GitFeature
import repo.build.CliOptions
import repo.build.RepoEnv

@CompileStatic
class PushFeatureCommand extends AbstractCommand {
    PushFeatureCommand() {
        super('push-feature', 'Push feature branches ')
    }

    void execute(RepoEnv env, CliOptions options) {
        GitFeature.pushFeatureBranch(env, options.getFeatureBranch(), true)
    }
}