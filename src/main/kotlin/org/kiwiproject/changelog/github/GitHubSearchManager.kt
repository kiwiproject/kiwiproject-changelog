package org.kiwiproject.changelog.github

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.annotations.VisibleForTesting
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.extension.getInt
import org.kiwiproject.changelog.extension.getListOfMaps
import org.kiwiproject.changelog.extension.getMap
import org.kiwiproject.changelog.extension.getString
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicInteger

class GitHubSearchManager(
    private val repoConfig: RepoConfig,
    private val api: GitHubApi,
    private val githubPagingHelper: GitHubPagingHelper,
    private val mapper: ObjectMapper
) {

    fun findIssuesByMilestone(milestoneTitle: String): List<GitHubIssue> {
        val allIssuesAndPulls = mutableListOf<GitHubIssue>()
        val totalCount = AtomicInteger()

        accumulateByType("issue", milestoneTitle, allIssuesAndPulls, totalCount)
        accumulateByType("pull-request", milestoneTitle, allIssuesAndPulls, totalCount)

        checkExpectedNumberOfIssues(allIssuesAndPulls.size, totalCount.get())

        return allIssuesAndPulls.sortedByDescending { it.createdAt }
    }

    private fun accumulateByType(
        type: String,
        milestoneTitle: String,
        allIssues: MutableList<GitHubIssue>,
        totalCount: AtomicInteger
    ) {
        val firstPageUrl = createSearchUrl(milestoneTitle, type)

        githubPagingHelper.paginate(api, firstPageUrl) { page, response ->
            val responseContent = mapper.readValue<Map<String, Any>>(response.content)

            if (page == 1) {
                totalCount.addAndGet(responseContent["total_count"] as Int)
            }

            val items = responseContent.getListOfMaps("items")
            val issues = items.map { item ->
                val title = item.getString("title")
                val number = item.getInt("number")
                val htmlUrl = item.getString("html_url")
                val createdAt = ZonedDateTime.parse(item.getString("created_at"))
                val labelNames = getLabels(item)

                val userMap = item.getMap("user")
                val login = userMap.getString("login")
                val userHtmlUrl = userMap.getString("html_url")
                val user = GitHubUser(login, login, userHtmlUrl)

                GitHubIssue(title, number, htmlUrl, labelNames, user, createdAt)
            }

            allIssues.addAll(issues)
        }
    }

    @VisibleForTesting
    internal fun checkExpectedNumberOfIssues(numIssues: Int, totalCount: Int) {
        check(numIssues == totalCount) { "Expected $totalCount issues but have $numIssues" }
    }

    private fun createSearchUrl(milestoneTitle: String, type: String): String =
        "${repoConfig.apiUrl}/search/issues?q=repo:${repoConfig.repository}+milestone:${milestoneTitle}+is:${type}&per_page=100&page=1"

    private fun getLabels(item: Map<String, Any>): List<String> {
        val labels = item.getListOfMaps("labels")
        return labels.map { it["name"] as String }
    }

    fun findUniqueAuthorsInCommitsBetween(base: String, head: String): CommitAuthorsResult {
        val authors = mutableSetOf<GitHubUser>()
        val firstPageUrl = createCompareCommitsUrl(base, head)
        var totalCommits = 0

        githubPagingHelper.paginate(api, firstPageUrl) { _, response ->
            val responseContent = mapper.readValue<Map<String, Any>>(response.content)

            val commitsOnPage = responseContent["total_commits"] as Int
            totalCommits += commitsOnPage

            val commits: List<Map<String, Any>> = responseContent.getListOfMaps("commits")
            val pageOfUniqueAuthors = commits.map(::gitHubUserFromCommit).toSet()

            authors.addAll(pageOfUniqueAuthors)
        }

        return CommitAuthorsResult(authors, totalCommits)
    }

    @VisibleForTesting
    internal fun gitHubUserFromCommit(commitContainer: Map<String, Any>): GitHubUser {
        // response schema:
        // commits/commit/author/name
        // commits/author/login
        // commits/author/html_url

        val authorName = getAuthorName(commitContainer)

        return if (commitContainer["author"] != null) {
            gitHubUserFrom(authorName, commitContainer)
        } else {
            println("WARN: Commit has null author: API: ${commitContainer["url"]} , HTML: ${commitContainer["html_url"]}")
            GitHubUser(authorName, null, null)
        }
    }

    private fun getAuthorName(commitContainer: Map<String, Any>): String {
        return commitContainer.getMap("commit").getMap("author").getString("name")
    }

    private fun gitHubUserFrom(authorName: String, commitContainer: Map<String, Any>): GitHubUser {
        val author = commitContainer.getMap("author")
        val login = author.getString("login")
        val htmlUrl = author.getString("html_url")
        return GitHubUser(authorName, login, htmlUrl)
    }

    data class CommitAuthorsResult(val authors: Set<GitHubUser>, val totalCommits: Int)

    private fun createCompareCommitsUrl(base: String, head: String): String =
        "${repoConfig.apiUrl}/repos/${repoConfig.repository}/compare/${base}...${head}?per_page=100&page=1"

    data class GitHubIssue(
        val title: String,
        val number: Int,
        val htmlUrl: String,
        val labels: List<String>,
        val user: GitHubUser?,
        val createdAt: ZonedDateTime
    )

    data class GitHubUser(
        val name: String,
        val login: String?,
        val htmlUrl: String?
    ) {

        fun asMarkdown(): String {
            return if (htmlUrl != null) "[$name](${htmlUrl})" else name
        }
    }
}
