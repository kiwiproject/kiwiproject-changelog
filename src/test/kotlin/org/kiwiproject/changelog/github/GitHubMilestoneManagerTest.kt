package org.kiwiproject.changelog.github

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwiproject.changelog.config.RepoHostConfig
import org.kiwiproject.changelog.extension.addGitHubRateLimitHeaders
import org.kiwiproject.changelog.extension.addJsonContentTypeHeader
import org.kiwiproject.changelog.extension.takeRequestWith1SecTimeout
import org.kiwiproject.changelog.extension.urlWithoutTrailingSlashAsString
import org.kiwiproject.test.util.Fixtures

@DisplayName("GitHubMilestoneManager")
class GitHubMilestoneManagerTest {

    private lateinit var server: MockWebServer
    private lateinit var milestoneManager: GitHubMilestoneManager
    private val mapper = jacksonObjectMapper()

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()

        val token = RandomStringUtils.randomAlphanumeric(40)
        val repoHostConfig = RepoHostConfig(
            "https://github.com",
            server.urlWithoutTrailingSlashAsString(),
            token,
            "sleberknight/kotlin-scratch-pad"
        )

        milestoneManager = GitHubMilestoneManager(repoHostConfig, GithubApi(token), mapper)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Nested
    inner class GetOpenMilestoneByTitle {

        @Test
        fun shouldGetMilestone() {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(getOpenMilestonesResponseJson())
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            val milestone = milestoneManager.getOpenMilestoneByTitle("0.9.0-alpha")

            assertMilestone(milestone)
            assertListMilestonesRequest()
        }

        @Test
        fun shouldThrowIllegalState_WhenMilestoneIsNotFound() {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(getOpenMilestonesResponseJson())
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            assertThatIllegalStateException()
                .isThrownBy { milestoneManager.getOpenMilestoneByTitle("0.10.0") }
                .withMessage("No milestone with title 0.10.0 was found")

            assertListMilestonesRequest()
        }

        @Test
        fun shouldThrowIllegalState_WhenListMilestoneRequestFails() {
            server.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("List milestones failed")
                    .addGitHubRateLimitHeaders()
            )

            assertThatIllegalStateException()
                .isThrownBy { milestoneManager.getOpenMilestoneByTitle("v0.9.0-alpha") }
                .withMessage("List milestones request failed. Status: 500. Text: List milestones failed")

            assertListMilestonesRequest()
        }

        @Test
        fun shouldThrowIllegalState_WhenGetLinkHeader() {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("[]")
                    .addHeader("Link", "")
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            assertThatIllegalStateException()
                .isThrownBy { milestoneManager.getOpenMilestoneByTitle("v0.9.0-alpha") }
                .withMessage("We received a Link header when we were not expecting it!")

            assertListMilestonesRequest()
        }
    }

    @Nested
    inner class GetOpenMilestoneByTitleOrNull {

        @Test
        fun shouldGetMilestone() {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(getOpenMilestonesResponseJson())
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            val milestone = milestoneManager.getOpenMilestoneByTitleOrNull("0.9.0-alpha")!!

            assertMilestone(milestone)
            assertListMilestonesRequest()
        }

        @Test
        fun shouldReturnNull_WhenDoesNotExist() {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(getOpenMilestonesResponseJson())
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            val title = "0.10.0"
            val milestone = milestoneManager.getOpenMilestoneByTitleOrNull(title)
            assertThat(milestone).isNull()

            assertListMilestonesRequest()
        }

        @Test
        fun shouldThrowIllegalState_WhenListMilestoneRequestFails() {
            server.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("List milestones failed")
                    .addGitHubRateLimitHeaders()
            )

            assertThatIllegalStateException()
                .isThrownBy { milestoneManager.getOpenMilestoneByTitleOrNull("v0.9.0-alpha") }
                .withMessage("List milestones request failed. Status: 500. Text: List milestones failed")

            assertListMilestonesRequest()
        }

        @Test
        fun shouldThrowIllegalState_WhenGetLinkHeader() {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("[]")
                    .addHeader("Link", "")
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            assertThatIllegalStateException()
                .isThrownBy { milestoneManager.getOpenMilestoneByTitleOrNull("v0.9.0-alpha") }
                .withMessage("We received a Link header when we were not expecting it!")

            assertListMilestonesRequest()
        }
    }

    private fun getOpenMilestonesResponseJson() =
        Fixtures.fixture("github-list-open-milestones-response.json")

    private fun assertListMilestonesRequest() {
        val listMilestonesRequest = server.takeRequestWith1SecTimeout()

        assertAll(
            { assertThat(listMilestonesRequest.method).isEqualTo("GET") },
            {
                assertThat(listMilestonesRequest.path)
                    .isEqualTo("/repos/sleberknight/kotlin-scratch-pad/milestones?state=open&per_page=50")
            }
        )
    }

    @Nested
    inner class CloseMilestone {

        @Test
        fun shouldCloseMilestone() {
            val closeResponseJson = Fixtures.fixture("github-close-milestone-response.json")

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(closeResponseJson)
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            val milestone = milestoneManager.closeMilestone(1)

            assertMilestone(milestone)
            assertCloseMilestoneRequest()
        }

        @Test
        fun shouldThrowIllegalState_WhenCloseMilestoneRequestFails() {
            server.enqueue(
                MockResponse()
                    .setResponseCode(422)
                    .setBody("Validation failed")
                    .addGitHubRateLimitHeaders()
            )

            assertThatIllegalStateException()
                .isThrownBy { milestoneManager.closeMilestone(1) }
                .withMessage("Close milestone 1 was unsuccessful. Status: 422. Text: Validation failed")
        }

        private fun assertCloseMilestoneRequest() {
            val closeMilestoneRequest = server.takeRequestWith1SecTimeout()
            assertAll(
                { assertThat(closeMilestoneRequest.method).isEqualTo("PATCH") },
                {
                    assertThat(closeMilestoneRequest.path)
                        .isEqualTo("/repos/sleberknight/kotlin-scratch-pad/milestones/1")
                },
                {
                    assertThat(closeMilestoneRequest.body.readUtf8())
                        .isEqualToIgnoringWhitespace(Fixtures.fixture("github-close-milestone-request.json"))
                }
            )
        }
    }

    private fun assertMilestone(milestone: GitHubMilestoneManager.GitHubMilestone) {
        assertAll(
            { assertThat(milestone.number).isEqualTo(1) },
            { assertThat(milestone.title).isEqualTo("0.9.0-alpha") },
            {
                assertThat(milestone.htmlUrl)
                    .isEqualTo("https://github.com/sleberknight/kotlin-scratch-pad/milestone/1")
            }
        )
    }

    @Nested
    inner class CreateNewMilestone {

        @Test
        fun shouldCreateNewMilestone() {
            val createMilestoneResponseJson = Fixtures.fixture("github-create-milestone-response.json")

            server.enqueue(
                MockResponse()
                    .setResponseCode(201)
                    .setBody(createMilestoneResponseJson)
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            val milestone = milestoneManager.createMilestone("0.9.0")

            assertAll(
                { assertThat(milestone.number).isEqualTo(2) },
                { assertThat(milestone.title).isEqualTo("0.9.0") },
                {
                    assertThat(milestone.htmlUrl)
                        .isEqualTo("https://github.com/sleberknight/kotlin-scratch-pad/milestone/2")
                }
            )

            assertCreateMilestoneRequest()
        }

        @Test
        fun shouldThrowIllegalState_WhenReceiveUnsuccessfulResponse() {
            server.enqueue(
                MockResponse()
                    .setResponseCode(422)
                    .setBody("Validation failed")
                    .addGitHubRateLimitHeaders()
            )

            assertThatIllegalStateException()
                .isThrownBy { milestoneManager.createMilestone("0.9.0") }
                .withMessage("Create milestone 0.9.0 was unsuccessful. Status: 422. Text: Validation failed")

            assertCreateMilestoneRequest()
        }

        private fun assertCreateMilestoneRequest() {
            val createMilestoneRequest = server.takeRequestWith1SecTimeout()
            assertAll(
                { assertThat(createMilestoneRequest.method).isEqualTo("POST") },
                {
                    assertThat(createMilestoneRequest.path)
                        .isEqualTo("/repos/sleberknight/kotlin-scratch-pad/milestones")
                },
                {
                    assertThat(createMilestoneRequest.body.readUtf8())
                        .isEqualToIgnoringWhitespace(Fixtures.fixture("github-create-milestone-request.json"))
                }
            )
        }
    }
}
