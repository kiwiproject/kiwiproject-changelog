package org.kiwiproject.changelog

import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.ConfigHelpers.buildCategoryConfig
import org.kiwiproject.changelog.config.ConfigHelpers.externalConfig
import org.kiwiproject.changelog.config.OutputType
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.config.RepoHostConfig
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.ITypeConverter
import picocli.CommandLine.IVersionProvider
import picocli.CommandLine.Option
import java.io.File
import kotlin.system.exitProcess

@Command(
    name = "ChangelogGenerator",
    versionProvider = ChangelogGeneratorMain.VersionProvider::class,
    mixinStandardHelpOptions = true,
    usageHelpAutoWidth = true,
    description = [
        "",
        "Generate a changelog from git commits between two revisions",
        ""]
)
class ChangelogGeneratorMain : Runnable {

    class VersionProvider : IVersionProvider {
        override fun getVersion(): Array<String> {
            val implVersion = ChangelogGeneratorMain::class.java.`package`.implementationVersion
            val version = implVersion ?: "[unknown]"
            return arrayOf(version)
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val exitCode = CommandLine(ChangelogGeneratorMain()).execute(*args)
            exitProcess(exitCode)
        }
    }

    // GitHub options
    @Option(
        names = ["-u", "--repo-host-url"],
        description = ["GitHub URL (default: \${DEFAULT-VALUE}))"],
        defaultValue = "https://github.com"
    )
    lateinit var repoHostUrl: String

    @Option(
        names = ["-a", "--repo-host-api-url"],
        description = ["URL for GitHub API (default: \${DEFAULT-VALUE})"],
        defaultValue = "https://api.github.com"
    )
    lateinit var repoHostApi: String

    // Authentication options
    @Option(
        names = ["-t", "--repo-host-token"],
        description = ["GitHub access token (can also be set via KIWI_CHANGELOG_TOKEN environment variable)"],
    )
    var token: String? = null

    // Repository options
    @Option(
        names = ["-r", "--repository"],
        description = ["Name of the GitHub repository (including organization)"],
        required = true
    )
    lateinit var repository: String

    @Option(
        names = ["-w", "--working-dir"],
        description = ["Working directory to run git commands (default: \${DEFAULT-VALUE})"],
        defaultValue = "."
    )
    lateinit var workingDir: String

    @Option(
        names = ["-p", "--previous-rev"],
        description = ["Starting revision commit to search for changes (default: \${DEFAULT-VALUE})"],
        required = true
    )
    lateinit var previousRevision: String

    @Option(
        names = ["-R", "--revision"],
        description = ["Ending revision commit to search for changes (default: \${DEFAULT-VALUE})"],
        defaultValue = "HEAD"
    )
    lateinit var revision: String

    // Output options

    @Option(
        names = ["-o", "--output-type"],
        description = ["How the changelog should be output: \${COMPLETION-CANDIDATES}"],
        converter = [OutputTypeConverter::class]
    )
    var outputType: OutputType = OutputType.CONSOLE

    class OutputTypeConverter : ITypeConverter<OutputType> {
        override fun convert(value: String): OutputType {
            try {
                return OutputType.valueOf(value.uppercase())
            } catch (e: Exception) {
                throw CommandLine.TypeConversionException(
                    "expected one of ${OutputType.entriesAsString()} (case-insensitive) but was '${value}'")
            }
        }
    }

    @Option(
        names = ["-f", "--output-file"],
        description = ["Location for file to output the change log. Only needed if output type is file."]
    )
    var outputFile: String? = null

    // Category Options
    @Option(
        names = ["-c", "--default-category"],
        description = ["Default category to put issues into if a mapping is not provided"],
    )
    var defaultCategory: String? = null

    @Option(
        names = ["-m", "--mapping"],
        description = ["Map a label to a category (format = label:category)"]
    )
    var labelToCategoryMappings: List<String> = listOf()

    @Option(
        names = ["-i", "--include-prs-from"],
        description = ["Always include PRs in the output from this user (e.g. dependabot)"]
    )
    var alwaysIncludePRsFrom: List<String> = listOf()

    @Option(
        names = ["-O", "--category-order"],
        description = ["The order to display the categories. Order is the order of these options"]
    )
    var categoryOrder: List<String> = listOf()

    @Option(
        names = ["-e", "--emoji-mapping"],
        description = ["Map a category to an emoji"]
    )
    var categoryToEmojiMappings: List<String> = listOf()

    // External configuration

    @Option(
        names = ["-n", "--config-file"],
        description = ["The configuration file to use"]
    )
    var configFile: String? = null

    @Option(
        names = ["-g", "--ignore-config-files"],
        description = ["When set, ignores standard configuration file locations"],
    )
    var ignoreConfigFiles: Boolean = false

    // Debug options

    @Option(
        names = ["-d", "--debug-args"],
        description = ["Prints the value of all arguments that would have been used, then exits"]
    )
    var debugArgs = false

    /**
     * Generate the changelog.
     */
    override fun run() {
        if (debugArgs) {
            printArgValues()
            return
        }

        val githubToken = token ?: System.getenv("KIWI_CHANGELOG_TOKEN")
        check(githubToken != null) {
            "GitHub token must be provided as command line option or KIWI_CHANGELOG_TOKEN environment variable"
        }

        // Get external configuration if one exists
        val currentDirectory = File(".").absoluteFile.parentFile
        val userHomeDirectory = File(System.getProperty("user.home"))
        val externalConfig = externalConfig(currentDirectory, userHomeDirectory, configFile, ignoreConfigFiles)

        val categoryConfig = buildCategoryConfig(
            labelToCategoryMappings,
            categoryToEmojiMappings,
            categoryOrder,
            defaultCategory,
            alwaysIncludePRsFrom,
            externalConfig
        )
        val repoHostConfig = RepoHostConfig(repoHostUrl, repoHostApi, githubToken, repository)
        val repoConfig = RepoConfig(File(workingDir), previousRevision, revision)

        if (outputType == OutputType.FILE) {
            check(outputFile != null) { "output file is required when output type is FILE" }
        }
        val out = outputFile?.let { File(it) }

        val changeLogConfig = ChangelogConfig(
            outputType = outputType,
            outputFile = out,
            categoryConfig = categoryConfig
        )

        GenerateChangelog(repoHostConfig, repoConfig, changeLogConfig).generate()
    }

    private fun printArgValues() {
        println()
        println("Arguments:")
        println("repoHostUrl = $repoHostUrl")
        println("repoHostApi = $repoHostApi")
        println("token = $token")
        println("repository = $repository")
        println("workingDir = $workingDir")
        println("previousRevision = $previousRevision")
        println("revision = $revision")
        println("outputType = $outputType")
        println("outputFile = $outputFile")
        println("defaultCategory = $defaultCategory")
        println("labelToCategoryMappings = $labelToCategoryMappings")
        println("categoryToEmojiMappings = $categoryToEmojiMappings")
        println("alwaysIncludePRsFrom = $alwaysIncludePRsFrom")
        println("categoryOrder = $categoryOrder")
        println("configFile = $configFile")
        println("----------")
    }
}
