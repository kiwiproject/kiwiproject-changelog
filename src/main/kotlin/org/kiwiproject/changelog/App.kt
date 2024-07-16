package org.kiwiproject.changelog

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.annotations.VisibleForTesting
import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.ConfigHelpers.buildCategoryConfig
import org.kiwiproject.changelog.config.ConfigHelpers.externalConfig
import org.kiwiproject.changelog.config.OutputType
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.github.GitHubApi
import org.kiwiproject.changelog.github.GitHubMilestoneManager
import org.kiwiproject.changelog.github.GitHubMilestoneManager.GitHubMilestone
import org.kiwiproject.changelog.github.GitHubPagingHelper
import org.kiwiproject.changelog.github.GitHubReleaseManager
import org.kiwiproject.changelog.github.GitHubSearchManager
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.ITypeConverter
import picocli.CommandLine.IVersionProvider
import picocli.CommandLine.Option
import java.io.File
import kotlin.system.exitProcess

@Command(
    name = "ChangelogGenerator",
    versionProvider = App.VersionProvider::class,
    mixinStandardHelpOptions = true,
    usageHelpAutoWidth = true,
    description = [
        "",
        "Generate a changelog for a specific GitHub milestone.",
        "",
        "Uses the GitHub REST API endpoints to search issues, pull requests, and commits.",
        "Composes the change log from issues and pull requests for a milestone.",
        "Uses the commits between revisions (tags) to find unique commit authors.",
        "",
        "Provides customization of the change log using labels associated with issues",
        "and pull requests, and lets you store that configuration in a YAML file.",
        "Labels can be associated with categories, and an emoji can be provided for",
        "each category. The category order can also be specified, as well as a default",
        "category when a label is not mapped to a category",
        "",
        "Provides various output options including to console, a file, or directly.",
        "to GitHub on the Releases page.",
        "",
        "You can also close the milestone on GitHub and/or create a new milestone.",
        "",
        "For more information, see the README at",
        "https://github.com/kiwiproject/kiwiproject-changelog",
        "",
        "Options:",
        ""
    ]
)
class App : Runnable {

    class VersionProvider : IVersionProvider {
        override fun getVersion(): Array<String> {
            val implVersion = App::class.java.`package`.implementationVersion
            val version = implVersion ?: "[unknown]"
            return arrayOf(version)
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
        names = ["-p", "--previous-rev"],
        description = ["Starting revision (tag) to search for commit authors"],
        required = true
    )
    lateinit var previousRevision: String

    @Option(
        names = ["-R", "--revision"],
        description = ["Ending revision (tag) to search for commit authors"],
        required = true
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

    //  Milestone options

    @Option(
        names = ["-C", "--close-milestone"],
        description = ["When set, the milestone associated with the revision is closed"]
    )
    var closeMilestone: Boolean = false

    @Option(
        names = ["-M", "--milestone"],
        description = [
            "This option lets you specify the milestone.",
            "If not specified, the milestone is assumed to be the revision without a leading 'v'.",
            "For example, the default milestone for revision v1.4.2 is 1.4.2"
        ]
    )
    var milestone: String? = null

    @Option(
        names = ["-N", "--create-next-milestone"],
        description = ["The title of the milestone to create, e.g., 4.2.0"]
    )
    var createNextMilestone: String? = null

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

        println("Generating change log for version $revision")

        val githubToken = token ?: System.getenv("KIWI_CHANGELOG_TOKEN")
        check(githubToken != null) {
            "GitHub token must be provided as command line option or KIWI_CHANGELOG_TOKEN environment variable"
        }

        // Get external configuration if one exists
        println("Getting configuration information")
        val currentDirectory = File(".").absoluteFile.parentFile
        val userHomeDirectory = File(System.getProperty("user.home"))
        val externalConfig = externalConfig(currentDirectory, userHomeDirectory, configFile, ignoreConfigFiles)

        val categoryConfig = buildCategoryConfig(
            labelToCategoryMappings,
            categoryToEmojiMappings,
            categoryOrder,
            defaultCategory,
            externalConfig
        )
        val repoConfig = RepoConfig(
            repoHostUrl,
            repoHostApi,
            githubToken,
            repository,
            previousRevision,
            revision,
            milestone
        )

        if (outputType == OutputType.FILE) {
            check(outputFile != null) { "output file is required when output type is FILE" }
        }
        val out = outputFile?.let { File(it) }

        val changeLogConfig = ChangelogConfig(
            outputType = outputType,
            outputFile = out,
            categoryConfig = categoryConfig
        )

        val githubApi = GitHubApi(githubToken)
        val mapper = jacksonObjectMapper()
        val releaseManager = GitHubReleaseManager(repoConfig, githubApi, mapper)
        val gitHubPagingHelper = GitHubPagingHelper()
        val searchManager = GitHubSearchManager(repoConfig, githubApi, gitHubPagingHelper, mapper)

        println("Gathering information for change log")
        ChangelogGenerator(repoConfig, changeLogConfig, releaseManager, searchManager).generate()

        // Optional: close the milestone
        val milestoneManager = GitHubMilestoneManager(repoConfig, githubApi, mapper)
        if (closeMilestone) {
            val closedMilestone = closeMilestone(repoConfig, milestone, milestoneManager)
            println("Closed milestone ${closedMilestone.title}. See it at ${closedMilestone.htmlUrl}")
        }

        // Optional: create new milestone
        if (createNextMilestone != null) {
            val newMilestone = createMilestone(createNextMilestone!!, milestoneManager)
            println("Created new milestone ${newMilestone.title}. See it at ${newMilestone.htmlUrl}")
        }
    }

    private fun printArgValues() {
        println()
        println("Arguments:")
        println("repoHostUrl = $repoHostUrl")
        println("repoHostApi = $repoHostApi")
        println("token = $token")
        println("repository = $repository")
        println("previousRevision = $previousRevision")
        println("revision = $revision")
        println("outputType = $outputType")
        println("outputFile = $outputFile")
        println("defaultCategory = $defaultCategory")
        println("labelToCategoryMappings = $labelToCategoryMappings")
        println("categoryToEmojiMappings = $categoryToEmojiMappings")
        println("categoryOrder = $categoryOrder")
        println("configFile = $configFile")
        println("----------")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val appResult = execute(args)
            exitProcess(appResult.exitCode)
        }

        @VisibleForTesting
        internal fun execute(args: Array<String>): AppResult {
            val app = App()
            val exitCode = CommandLine(app).execute(*args)
            return AppResult(exitCode, app)
        }

        @VisibleForTesting
        internal data class AppResult(val exitCode: Int, val app: App)

        @VisibleForTesting
        fun closeMilestone(
            repoConfig: RepoConfig,
            maybeMilestoneTitle: String?,
            milestoneManager: GitHubMilestoneManager
        ): GitHubMilestone {
            val milestoneTitle = maybeMilestoneTitle ?: repoConfig.milestone()
            println("Closing milestone $milestoneTitle")

            val milestone = milestoneManager.getOpenMilestoneByTitle(milestoneTitle)
            val closedMilestone = milestoneManager.closeMilestone(milestone.number)

            return closedMilestone
        }

        @VisibleForTesting
        fun createMilestone(title: String, milestoneManager: GitHubMilestoneManager): GitHubMilestone {
            println("Creating new milestone $title")

            val maybeMilestone = milestoneManager.getOpenMilestoneByTitleOrNull(title)
            if (maybeMilestone != null) {
                println("Milestone $title already exists. Returning it.")
                return maybeMilestone
            }

            return milestoneManager.createMilestone(title)
        }
    }
}
