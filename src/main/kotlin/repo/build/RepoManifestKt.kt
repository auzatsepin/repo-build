package repo.build

import groovy.util.Node

class ManifestFilter(val action: (ActionContext, Node) -> Boolean) {

    operator fun invoke(context: ActionContext, project: Node): Boolean {
        return action(context, project)
    }

}

open class ManifestAction(val action: (ActionContext, Node) -> Unit) {

    operator fun invoke(context: ActionContext, project: Node) {
        action(context, project)
    }

}

class ManifestLogHeader(action: (ActionContext, Node) -> Unit) : ManifestAction(action)

class ManifestLogFooter(action: (ActionContext, Node) -> Unit) : ManifestAction(action)