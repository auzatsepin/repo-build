package repo.build

import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.junit.After
import org.junit.Before
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files

abstract class BaseTestCaseKt {

    lateinit var sandbox : SandboxKt
    lateinit var env : RepoEnv
    lateinit var options : CliOptions
    lateinit var context : ActionContext
    val mavenReader = MavenXpp3Reader()

    @Before
    open fun setUp() {
        env = RepoEnv(createTempDir())
        val cli = CliBuilderFactory.build(null)
        options = CliOptions(cli.parse(getArgs()))
        context = ActionContext(env, null, options, DefaultActionHandler())
    }

    @After
    open fun tearDown() {
        try {
            context.close()
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    private fun getArgs() = listOf("-j", "4")

    private fun createTempDir() : File {
        return Files.createTempDirectory(
                FileSystems.getDefault().getPath("target"), "sandbox").toFile()
    }
}