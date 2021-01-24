package org.kiwiproject.changelog

import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.GitRepoConfig
import org.kiwiproject.changelog.config.GithubConfig
import org.kiwiproject.changelog.config.OutputType
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

class GenerateChangelog(
    private val githubConfig: GithubConfig,
    private val gitRepoConfig: GitRepoConfig,
    private val changeLogConfig: ChangelogConfig
) {

    fun generate() {
        println("Finding commits between ${gitRepoConfig.previousRevision}..${gitRepoConfig.revision} in dir: ${gitRepoConfig.workingDir}")
        val commits = commits()

        println("Collecting ticket ids from ${commits.size} commits.")
        val tickets : List<String> = commits.flatMap(GitCommit::tickets)
        val contributors : Set<String> = commits.map(GitCommit::author).toSet()

        println("Fetching ticket info from ${githubConfig.fullRepoUrl()} based on ${tickets.size} ids $tickets")
        val improvements = GithubTicketFetcher(githubConfig, changeLogConfig).fetchTickets(tickets)

        println("Generating changelog based on ${improvements.size} tickets from Github")
        val githubUrl = "${githubConfig.url}/${githubConfig.repository}"
        val changeLog = formatChangeLog(contributors, improvements, commits.size, gitRepoConfig, changeLogConfig, githubUrl)

        println("Writing out changelog")

        when(changeLogConfig.outputType) {
            OutputType.CONSOLE -> println(changeLog)
            OutputType.FILE -> writeFile(changeLog)
            OutputType.GITHUB_RELEASE -> println(changeLog) // TODO: Implement this
        }
    }

    private fun commits() : List<GitCommit> {
        println("Loading all commits between ${gitRepoConfig.previousRevision} and ${gitRepoConfig.revision}")

        val infoToken = "@@info@@"
        val commitToken = "@@commit@@"

        // %H: commit hash
        // %ae: author email
        // %an: author name
        // %B: raw body (unwrapped subject and body)
        // %N: commit notes
        val log = GitLogProvider(gitRepoConfig.workingDir).getLog(gitRepoConfig.previousRevision,
            gitRepoConfig.revision, "--pretty=format:%H$infoToken%ae$infoToken%an$infoToken%B%N$commitToken")

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