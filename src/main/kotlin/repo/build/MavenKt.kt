package repo.build

import org.apache.maven.shared.invoker.InvocationRequest
import org.apache.maven.shared.invoker.InvocationResult

class MavenRequest(val action: (InvocationRequest) -> Unit) {

    operator fun invoke(request: InvocationRequest) {
        action(request)
    }

}

class MavenResult(val action: (InvocationResult) -> Unit) {

    operator fun invoke(result: InvocationResult) {
        action(result)
    }

}