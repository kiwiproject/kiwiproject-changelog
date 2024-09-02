package org.kiwiproject.changelog.github

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwiproject.changelog.MockWebServerExtension
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.extension.addGitHubRateLimitHeaders
import org.kiwiproject.changelog.extension.addJsonContentTypeHeader
import org.kiwiproject.changelog.extension.assertNoMoreRequests
import org.kiwiproject.changelog.extension.takeRequestWith1SecTimeout
import org.kiwiproject.changelog.extension.urlWithoutTrailingSlashAsString
import org.kiwiproject.test.junit.jupiter.ClearBoxTest
import org.kiwiproject.test.util.Fixtures

@DisplayName("GitHubSearchManager")
class GitHubSearchManagerTest {

    private lateinit var server: MockWebServer
    private lateinit var searchManager: GitHubSearchManager
    private val mapper = jacksonObjectMapper()

    @RegisterExtension
    val mockWebServerExtension = MockWebServerExtension()

    @BeforeEach
    fun setUp() {
        server = mockWebServerExtension.server

        val token = RandomStringUtils.secure().nextAlphanumeric(40)
        val repoConfig = RepoConfig(
            "https://fake-github.com",
            server.urlWithoutTrailingSlashAsString(),
            token,
            "kiwiproject/kiwiproject-changelog",
            "v0.11.0",
            "v0.12.0",
            milestone = null
        )

        searchManager = GitHubSearchManager(repoConfig, GitHubApi(token), GitHubPagingHelper(), mapper)
    }

    @Nested
    inner class FindIssuesByMilestone {

        @Test
        fun shouldFindIssuesAndPullRequestsForTheMilestone() {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(getIssueSearchResponseJson())
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(getPullRequestSearchResponseJson())
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            val issues = searchManager.findIssuesByMilestone("0.12.0")

            assertAll(
                { assertThat(issues.size).isEqualTo(9) },
                {
                    assertThat(issues)
                        .extracting("title", "number", "labels")
                        .contains(
                            tuple(
                                "Use BOMs for Jackson and Kotlin to prevent dependency convergence errors",
                                193,
                                listOf("enhancement", "dependencies")
                            ),
                            tuple(
                                "Bump org.kiwiproject:kiwi-parent from 3.0.15 to 3.0.16",
                                189,
                                listOf("dependencies", "java")
                            ),
                            tuple(
                                "Add curl-based install instructions to the README",
                                185,
                                listOf("documentation")
                            )
                        )
                }
            )

            val issueRequest = server.takeRequestWith1SecTimeout()
            assertAll(
                { assertThat(issueRequest.method).isEqualTo("GET") },
                {
                    assertThat(issueRequest.path).isEqualTo(
                        "/search/issues?q=repo:kiwiproject/kiwiproject-changelog+milestone:0.12.0+is:issue&per_page=100&page=1"
                    )
                }
            )

            val pullRequestsRequest = server.takeRequestWith1SecTimeout()
            assertAll(
                { assertThat(pullRequestsRequest.method).isEqualTo("GET") },
                {
                    assertThat(pullRequestsRequest.path).isEqualTo(
                        "/search/issues?q=repo:kiwiproject/kiwiproject-changelog+milestone:0.12.0+is:pull-request&per_page=100&page=1"
                    )
                }
            )

            server.assertNoMoreRequests()
        }

        private fun getIssueSearchResponseJson(): String =
            Fixtures.fixture("github-search-issues-for-milestone.json")

        private fun getPullRequestSearchResponseJson(): String =
            Fixtures.fixture("github-search-pull-requests-for-milestone.json")

        @Test
        fun shouldPaginate() {
            // One "type:issue" request
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(getIssuesSearchResponseJsonForPageOne())
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            // Two "type:pull-request" requests

            // Add the "Link" header and inject the MockWebServer URL
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(getPullRequestSearchResponseJsonForPage(1))
                    .addHeader(
                        "Link",
                        "<${server.urlWithoutTrailingSlashAsString()}/search/issues?q=repo:kiwiproject%2Fchampagne-service+milestone:0.1.0+is:pull-request&per_page=5&page=2>; rel=\"next\", <https://api.github.com/search/issues?q=repo:kiwiproject%2Fchampagne-service+milestone:0.1.0&per_page=5&page=93>; rel=\"last\""
                    )
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(getPullRequestSearchResponseJsonForPage(2))
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            val issues = searchManager.findIssuesByMilestone("0.12.0")
            assertThat(issues).hasSize(15)

            // Verify we sent two requests
            val request1 = server.takeRequestWith1SecTimeout()
            val request2 = server.takeRequestWith1SecTimeout()
            val request3 = server.takeRequestWith1SecTimeout()
            assertAll(
                { assertThat(request1.method).isEqualTo("GET") },
                {
                    assertThat(request1.path)
                        .startsWith("/search/issues")
                        .contains("is:issue")
                        .endsWith("page=1")
                },
                { assertThat(request2.method).isEqualTo("GET") },
                {
                    assertThat(request2.path)
                        .startsWith("/search/issues")
                        .contains("is:pull-request")
                        .endsWith("page=1")
                },
                { assertThat(request3.method).isEqualTo("GET") },
                {
                    assertThat(request3.path)
                        .startsWith("/search/issues")
                        .contains("is:pull-request")
                        .endsWith("page=2")
                }
            )
            server.assertNoMoreRequests()
        }

        private fun getIssuesSearchResponseJsonForPageOne(): String =
            Fixtures.fixture("github-search-issues-for-milestone-page-1.json")

        private fun getPullRequestSearchResponseJsonForPage(page: Int): String =
            Fixtures.fixture("github-search-pull-requests-for-milestone-page-$page.json")

        @ClearBoxTest
        fun shouldCheckExpectedNumberOfIssues() {
            assertAll(
                {
                    assertThatCode { searchManager.checkExpectedNumberOfIssues(10, 10) }
                        .doesNotThrowAnyException()
                },
                {
                    assertThatIllegalStateException()
                        .isThrownBy { searchManager.checkExpectedNumberOfIssues(10, 11) }
                },
                {
                    assertThatIllegalStateException()
                        .isThrownBy { searchManager.checkExpectedNumberOfIssues(20, 19) }
                }
            )
        }
    }

    @Nested
    inner class UniqueAuthorsInCommitsBetween {

        @Test
        fun shouldFindTheUniqueAuthors() {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(getCompareCommitsResponseJson())
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            val authorsResult = searchManager.findUniqueAuthorsInCommitsBetween("v0.11.0", "v0.12.0")

            assertAll(
                { assertThat(authorsResult.totalCommits).isEqualTo(10) },
                { assertThat(authorsResult.authors).hasSize(2) },
                {
                    assertThat(authorsResult.authors)
                        .extracting("name", "login")
                        .containsOnly(
                            tuple("Scott Leberknight", "sleberknight"),
                            tuple("dependabot[bot]", "dependabot[bot]")
                        )
                }
            )

            val request = server.takeRequestWith1SecTimeout()
            assertAll(
                { assertThat(request.method).isEqualTo("GET") },
                {
                    assertThat(request.path).isEqualTo(
                        "/repos/kiwiproject/kiwiproject-changelog/compare/v0.11.0...v0.12.0?per_page=100&page=1"
                    )
                }
            )

            server.assertNoMoreRequests()
        }

        private fun getCompareCommitsResponseJson() =
            Fixtures.fixture("github-get-compare-commits.json")

        @Nested
        inner class GitHubUserFromCommit {

            @Test
            fun shouldReturnCompleteGitHubUser() {
                val commitContainer = mapOf<String, Any>(
                    "commit" to mapOf<String, Any>(
                        "author" to mapOf<String, Any>(
                            "name" to "Jack Tripper"
                        )
                    ),
                    "author" to mapOf<String, Any>(
                        "login" to "jack_tripper",
                        "html_url" to "https://fake-github.com/jack_tripper"
                    )
                )

                val user = searchManager.gitHubUserFromCommit(commitContainer)

                assertAll(
                    { assertThat(user.name).isEqualTo("Jack Tripper") },
                    { assertThat(user.login).isEqualTo("jack_tripper") },
                    { assertThat(user.htmlUrl).isEqualTo("https://fake-github.com/jack_tripper") }
                )
            }

            @Test
            fun shouldReturnGitHubUserWithoutLoginOrHtmlUrl_WhenTopLevelAuthorPropertyIsNull() {
                // Note:
                // We must declare the map value type as Any? (nullable) and then
                // use an unchecked cast later when passing it as an argument.
                val commitContainer = mapOf<String, Any?>(
                    "commit" to mapOf<String, Any>(
                        "author" to mapOf<String, Any>(
                            "name" to "Bob Kramer"
                        )
                    ),
                    "author" to null
                )

                @Suppress("UNCHECKED_CAST")
                val user = searchManager.gitHubUserFromCommit(commitContainer as Map<String, Any>)

                assertAll(
                    { assertThat(user.name).isEqualTo("Bob Kramer") },
                    { assertThat(user.login).isNull() },
                    { assertThat(user.htmlUrl).isNull() }
                )
            }
        }
    }

    @Nested
    inner class GitHubUserDataClass {

        @Test
        fun shouldRenderAsMarkdownLink_WhenHtmlUrlIsPresent() {
            val user = GitHubUser("Bob", "bob42", "https://fake-github.com/bob42")

            assertThat(user.asMarkdown()).isEqualTo("[Bob](https://fake-github.com/bob42)")
        }

        @Test
        fun shouldRenderAsPlainText_WhenHtmlUrlIsAbsent() {
            val user = GitHubUser("Bob", null, null)

            assertThat(user.asMarkdown()).isEqualTo("Bob")
        }
    }
}
