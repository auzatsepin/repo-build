package repo.build

import java.io.File
import java.io.FileWriter
import java.util.regex.Pattern

class SandboxClosure(val action: (Sandbox, File?) -> Sandbox) {

    operator fun invoke(sandbox: Sandbox, dir: File?): Sandbox {
        return action(sandbox, dir)
    }

}

typealias SandboxAction = (SandboxKt, File) -> SandboxKt

class SandboxKt {

    val env: RepoEnv
    private val components: MutableMap<String, File>
    val context: ActionContext

    constructor(env: RepoEnv, options: CliOptions) {
        this.env = env
        this.components = mutableMapOf()
        this.context = ActionContext(env, null, options, DefaultActionHandler())
    }

    fun newGitComponent(component: String, action: SandboxAction): SandboxKt {
        val dir = File(env.basedir, component)
        dir.mkdirs()
        Git.init(context, dir)
        Git.user(context, dir, "you@example.com", "Your Name")
        action(this, dir)
        components[component] = dir
        return this
    }

    fun newGitComponent(component: String): SandboxKt {
        return newGitComponent(component, action = { sandboxKt, dir -> sandboxKt.gitInitialCommit(dir) })
    }

    fun component(component: String, action: SandboxAction): SandboxKt {
        val componentDir = components[component] ?: throw RuntimeException("Component $component not found in context")
        action(this, componentDir)
        return this
    }

    fun gitInitialCommit(dir: File): SandboxKt {
        val readme = File(dir, "README.md")
        readme.createNewFile()
        Git.add(context, dir, readme.canonicalPath)
        Git.commit(context, dir, "init")
        return this
    }

    fun buildManifest(dir: File) {
        //todo xml writer with pretty print, like data class for gson/jackson
        val manifest = """
            |<manifest>
            |    <remote name='origin' fetch='${dir.parentFile.absolutePath}' />
            |    <default revision='refs/heads/develop' remote='origin' sync='1' />
            |    ${components.map { "<project name='${it.key}' remote='origin' path='${it.key}' revision='refs/heads/master' />" }.joinToString("\n")}
            |</manifest>
        """.trimMargin("|")
        val fileWriter = FileWriter(File(dir, "default.xml"))
        fileWriter.write(manifest)
        fileWriter.close()

    }

    fun changeDefaultBranchComponentOnManifest(manifestDir: File, component: String, defaultBranch: String): SandboxKt {
        val manifestFileName = if (manifestDir.endsWith("/")) {
            "default.xml"
        } else {
            "/default.xml"
        }
        val manifestFile = File(manifestDir, manifestFileName)
        val text = manifestFile.readText(Charsets.UTF_8)
        val origin = "<project name='$component' remote='origin' path='$component' revision='refs/heads/[\\w\\s./]+' />"
        val new = "<project name='$component' remote='origin' path='$component' revision='refs/heads/$defaultBranch' />"
        val pattern = Pattern.compile(origin)
        val matcher = pattern.matcher(text)
        val replaceAll = matcher.replaceAll(new)
        manifestFile.writeText(replaceAll, Charsets.UTF_8)
        return this
    }

}
