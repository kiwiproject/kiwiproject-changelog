package org.kiwiproject.changelog

import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.OutputType
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.config.RepoHostConfig
import org.kiwiproject.changelog.github.GithubTicketFetcher
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

class GenerateChangelog(
    private val repoHostConfig: RepoHostConfig,
    private val repoConfig: RepoConfig,
    private val changeLogConfig: ChangelogConfig
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

        when(changeLogConfig.outputType) {
            OutputType.CONSOLE -> println(changeLog)
            OutputType.FILE -> writeFile(changeLog)
            OutputType.GITHUB -> {  // TODO: Implement this
                println("*** GitHub Release Not Implemented Yet. Printing to console. ***")
                println(changeLog)
            }
        }
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

    private fun writeFile(content: String) {
        val out = changeLogConfig.outputFile ?: return

        out.parentFile?.mkdirs()
        try {
            PrintWriter(OutputStreamWriter(FileOutputStream(out), StandardCharsets.UTF_8)).use { p ->
                p.write(content)
            }
        } catch (e: Exception) {
            throw RuntimeException("Problems writing text to file: $out", e)
        }
    }
}
