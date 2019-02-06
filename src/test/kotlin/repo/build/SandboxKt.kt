package repo.build

import java.io.File


class SandboxClosure(val action: (Sandbox, File?) -> Sandbox) {

    operator fun invoke(sandbox: Sandbox, dir: File?): Sandbox {
        return action(sandbox, dir)
    }

}

/*
import org.redundent.kotlin.xml.xml
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths

typealias SandboxAction = (SandboxKt, File) -> Unit

class SandboxKt*/
/*(private val env: RepoEnv,
                private val options: CliOptions,
                private val components: MutableMap<String, File> = mutableMapOf(),
                private val context = ActionContext(env, null, options, new DefaultActionHandler()))*//*
 {

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
        return newGitComponent(component) { sandboxKt, dir -> sandboxKt.gitInitialCommit(dir) }
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
        val manifest = xml("manifest") {
            "remote" {
                attributes(
                        "name" to "origin",
                        "fetch" to dir.parentFile.absoluteFile
                )
            }
            "default" {
                attributes(
                        "revision" to "refs/heads/develop",
                        "remote" to "origin",
                        "sync" to 1
                )
            }
            components.forEach {
                "project" {
                    attributes(
                            "name" to it.key,
                            "remote" to "origin",
                            "path" to it.key,
                            "revision" to "refs/heads/master"
                    )

                }
            }
        }
        FileWriter(File(dir, "default.xml")).use {
            manifest.toString(true)
        }
    }

    fun changeDefaultBranchComponentOnManifest(manifestDir: File, component: String, defaultBranch: String): SandboxKt {
        val toString = Files.readAllBytes(Paths.get(if (manifestDir.endsWith("/")) {
            manifestDir.absolutePath + "default.xml"
        } else {
            manifestDir.absolutePath + "/default.xml"
        })).toString(Charsets.UTF_8)
        toString.replace("""<project name='$component' remote='origin' path='$component' revision='refs/heads/[\\w\\s./]+\' />'""",
                """'<project name='$component' remote='origin' path='$component' revision='refs/heads/$defaultBranch' />""".trimMargin())
        return this
    }

}*/
