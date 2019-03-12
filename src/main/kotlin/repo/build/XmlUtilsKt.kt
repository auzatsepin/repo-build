package repo.build

import org.w3c.dom.Node

class XmlUtilsAction(val action: (Node) -> Unit) {

    operator fun invoke(node: Node) {
        return action(node)
    }

}