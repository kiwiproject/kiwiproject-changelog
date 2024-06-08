package org.kiwiproject.changelog

import com.google.common.annotations.VisibleForTesting
import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.OutputType
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.config.RepoHostConfig
import org.kiwiproject.changelog.github.GitHubReleaseManager
import org.kiwiproject.changelog.github.GithubTicketFetcher
import java.io.File

class GenerateChangelog(
    private val repoHostConfig: RepoHostConfig,
    private val repoConfig: RepoConfig,
    private val changeLogConfig: ChangelogConfig,
    private val releaseManager: GitHubReleaseManager
) {

    fun generate() {
        println("Finding commits between ${repoConfig.previousRevision}..${repoConfig.revision} in dir: ${repoConfig.workingDir}")
        val commits = commits()

        println("Collecting ticket ids from ${commits.size} commits.")
        val tickets : List<String> = commits.flatMap(GitCommit::tickets)
        val contributors : Set<String> = commits.map(GitCommit::author).toSet()

        println("Fetching ticket info from ${repoHostConfig.fullRepoUrl()} based on ${tickets.size} ids $tickets")
        val improvements = GithubTicketFetcher(repoHostConfig, changeLogConfig).fetchTickets(tickets)

        println("Generating changelog based on ${improvements.size} tickets from GitHub")
        val githubUrl = "${repoHostConfig.url}/${repoHostConfig.repository}"
        val changeLog = formatChangeLog(contributors, improvements, commits.size, repoConfig, changeLogConfig, githubUrl)

        println("Writing out changelog to ${changeLogConfig.outputType}")
        writeChangeLog(changeLog)
    }

    private fun commits() : List<GitCommit> {
        println("Loading all commits between ${repoConfig.previousRevision} and ${repoConfig.revision}")

        val infoToken = "@@info@@"
        val commitToken = "@@commit@@"

        // %H: commit hash
        // %ae: author email
        // %an: author name
        // %B: raw body (unwrapped subject and body)
        // %N: commit notes
        val log = GitLogProvider(repoConfig.workingDir).getLog(repoConfig.previousRevision,
            repoConfig.revision, "--pretty=format:%H$infoToken%ae$infoToken%an$infoToken%B%N$commitToken")

        return log.split(commitToken)
            .map { entry -> entry.split(infoToken) }
            .filter { parts -> parts.size == 4 }
            .map { parts -> GitCommit(parts[2].trim(), parts[3].trim()) }
    }

    @VisibleForTesting
    fun writeChangeLog(changeLog: String) {
        when (changeLogConfig.outputType) {
            OutputType.CONSOLE -> {
                println("---------- Change log ---------- ")
                println(changeLog)
                println("---------- End change log ---------- ")
            }
            OutputType.FILE -> {
                writeFile(changeLog)
                println("Wrote changelog to ${changeLogConfig.outputFile}")
            }
            OutputType.GITHUB -> {
                val release = releaseManager.createRelease(repoConfig.revision, changeLog)
                println("Created GitHub release. See it at ${release.htmlUrl}")
            }
        }
    }

    private fun writeFile(content: String): File {
        checkNotNull(changeLogConfig.outputFile) {
            "changeLogConfig.outputFile must not be null." +
                    " The --output-file (or -f) option is required when output type is file."
        }
        writeFile(content, changeLogConfig.outputFile)
        return changeLogConfig.outputFile
    }

    @VisibleForTesting
    fun writeFile(content: String, out: File) {
        out.parentFile?.mkdirs()
        out.writeText(content)
    }
}
