package repo.build

import groovy.transform.CompileStatic
import kotlin.Unit
import kotlin.jvm.functions.Function1
import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.DefaultInvoker
import org.apache.maven.shared.invoker.InvocationRequest
import org.apache.maven.shared.invoker.InvocationResult
import org.apache.maven.shared.invoker.Invoker

/**
 */
class Maven {

    static void execute(ActionContext context, File pomFile, MavenRequest handleRequest, MavenResult handleResult) {
        InvocationRequest request = new DefaultInvocationRequest()
        request.setPomFile(pomFile)
        handleRequest.invoke(request)

        Invoker invoker = new DefaultInvoker()
        try {
            InvocationResult result = invoker.execute(request)
            if (result.exitCode != 0) {
                throw new RepoBuildException("exitCode: " + result.exitCode)
            }
            handleResult.invoke(result)
        }
        catch (Exception e) {
            throw new RepoBuildException(e.getMessage(), e)
        }
    }

    static void execute(ActionContext context, File pomFile, MavenRequest handleRequest) {
        execute(context, pomFile, handleRequest, new MavenResult(
                new Function1<InvocationResult, Unit>() {
                    @Override
                    Unit invoke(InvocationResult invocationResult) {
                        return null
                    }
                }
        ))
    }

    @CompileStatic
    static void execute(ActionContext context,
                        File pomFile,
                        List<String> goals,
                        Map<String, String> p) {
        execute(context, pomFile,
                new MavenRequest(
                        new Function1<InvocationRequest, Unit>() {
                            @Override
                            Unit invoke(InvocationRequest req) {
                                MavenFeature.initInvocationRequest(req, context.getOptions())
                                req.setGoals(goals)
                                req.setInteractive(false)
                                req.getProperties().putAll(p)
                                return null
                            }
                        }
                )
        )
    }

    @CompileStatic
    static void execute(ActionContext context,
                        File pomFile,
                        List<String> goals) {
        execute(context, pomFile, goals, [:])
    }

}
