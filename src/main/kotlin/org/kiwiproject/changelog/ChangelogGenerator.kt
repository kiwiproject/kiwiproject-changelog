package org.kiwiproject.changelog

import com.google.common.annotations.VisibleForTesting
import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.OutputType
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.github.GitHubCategoryFinder
import org.kiwiproject.changelog.github.GitHubChange
import org.kiwiproject.changelog.github.GitHubReleaseManager
import org.kiwiproject.changelog.github.GitHubSearchManager
import java.io.File

class ChangelogGenerator(
    private val repoConfig: RepoConfig,
    private val changeLogConfig: ChangelogConfig,
    private val releaseManager: GitHubReleaseManager,
    private val searchManager: GitHubSearchManager
) {

    fun generate(): GenerateResult {
        println("🔎 Finding commits between ${repoConfig.previousRevision}..${repoConfig.revision}")
        val commitAuthorsResult =
            searchManager.findUniqueAuthorsInCommitsBetween(repoConfig.previousRevision, repoConfig.revision)

        val categoryFinder = GitHubCategoryFinder(changeLogConfig.categoryConfig)
        val issues = searchManager.findIssuesByMilestone(repoConfig.milestone())
        val changes = issues.map { issue ->
            val category = categoryFinder.findCategory(issue.labels)
            GitHubChange.from(issue, category)
        }

        println("🛠 Generating changelog based on ${changes.size} issues and PRs from GitHub for milestone ${repoConfig.milestone()}")
        val githubUrl = repoConfig.repoUrl()
        val changeLog = formatChangeLog(commitAuthorsResult, changes, repoConfig, changeLogConfig, githubUrl)

        println("📝 Writing out changelog to ${changeLogConfig.outputType}")
        writeChangeLog(changeLog)

        return GenerateResult(
            commitAuthorsResult.authors.size,
            commitAuthorsResult.totalCommits,
            changes.size,
            changeLog
        )
    }

    data class GenerateResult(
        val uniqueAuthorCount: Int,
        val commitCount: Int,
        val changeCount: Int,
        val changelogText: String
    )

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
                println("✅ Wrote changelog to ${changeLogConfig.outputFile}")
            }

            OutputType.GITHUB -> {
                val release = releaseManager.createRelease(repoConfig.revision, changeLog)
                println("✅ Created GitHub release. See it at ${release.htmlUrl}")
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
