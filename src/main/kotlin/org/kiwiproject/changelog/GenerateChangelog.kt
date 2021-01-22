package org.kiwiproject.changelog

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.lang.Exception
import java.nio.charset.StandardCharsets

class GenerateChangelog(
    val githubUrl: String,
    val workingDir: File,
    val githubToken: String,
    val githubApiUrl: String,
    val repository: String,
    val previousRevision: String = "master",
    val version: String,
    val revision: String,
    val date: String,
    val outputFile: String
) {

    fun generate() {
        println("Finding commits between $previousRevision..$revision in dir: $workingDir")
        val commits = commits()

        println("Collecting ticket ids from ${commits.size} commits.")
        val tickets : List<String> = commits.flatMap(GitCommit::tickets)
        val contributors : Set<String> = commits.map(GitCommit::author).toSet()

        println("Fetching ticket info from $githubApiUrl/$repository based on ${tickets.size} ids $tickets")
        val improvements = GithubTicketFetcher(githubApiUrl, repository, githubToken).fetchTickets(tickets)

        println("Generating changelog based on ${improvements.size} tickets from Github")
        val changeLog = formatChangeLog(contributors, improvements, commits.size, version, previousRevision, "$githubUrl/$repository", date)

        println("Writing out changelog")

        if (outputFile == "CONSOLE") {
            println(changeLog)
        } else {
            writeFile(changeLog)
        }
    }

    private fun commits() : List<GitCommit> {
        println("Loading all commits between $previousRevision and $revision")

        val infoToken = "@@info@@"
        val commitToken = "@@commit@@"

        // %H: commit hash
        // %ae: author email
        // %an: author name
        // %B: raw body (unwrapped subject and body)
        // %N: commit notes
        val log = GitLogProvider(workingDir).getLog(previousRevision, revision, "--pretty=format:%H$infoToken%ae$infoToken%an$infoToken%B%N$commitToken")

        return log.split(commitToken)
            .map { entry -> entry.split(infoToken) }
            .filter { parts -> parts.size == 4 }
            .map { parts -> GitCommit(parts[2].trim(), parts[3].trim()) }
    }

    private fun writeFile(content: String) {
        val out = File(outputFile)
        out.parentFile?.mkdirs()
        try {
            PrintWriter(OutputStreamWriter(FileOutputStream(out), StandardCharsets.UTF_8)).use { p ->
                p.write(content)
            }
        } catch (e: Exception) {
            throw RuntimeException("Problems writing text to file: $outputFile", e)
        }
    }
}