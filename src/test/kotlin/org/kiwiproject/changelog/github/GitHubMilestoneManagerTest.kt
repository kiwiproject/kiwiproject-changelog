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

    @Test
    fun shouldGetOpenMilestoneByTitle() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getOpenMilestonesResponseJson())
                .addGitHubRateLimitHeaders()
        )

        val title = "0.9.0-alpha"
        val milestone = milestoneManager.getOpenMilestoneByTitle(title)

        assertAll(
            { assertThat(milestone.number).isEqualTo(1) },
            { assertThat(milestone.title).isEqualTo(title) },
            {
                assertThat(milestone.htmlUrl)
                    .isEqualTo("https://github.com/sleberknight/kotlin-scratch-pad/milestone/1")
            }
        )

        assertListMilestonesRequest()
    }

    @Test
    fun shouldThrowIllegalState_IfMilestoneIsNotFound_WhenGettingOpenMilestoneByTitle() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getOpenMilestonesResponseJson())
                .addGitHubRateLimitHeaders()
        )

        assertThatIllegalStateException()
            .isThrownBy { milestoneManager.getOpenMilestoneByTitle("0.10.0") }
            .withMessage("No milestone with title 0.10.0 was found")

        assertListMilestonesRequest()
    }

    @Test
    fun shouldThrowIllegalState_IfListMilestoneRequestFails_WhenGettingOpenMilestoneByTtile() {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("List milestones failed")
                .addJsonContentTypeHeader()
                .addGitHubRateLimitHeaders()
        )

        assertThatIllegalStateException()
            .isThrownBy { milestoneManager.getOpenMilestoneByTitle("v0.9.0-alpha") }
            .withMessage("List milestones request failed. Status: 500. Text: List milestones failed")

        assertListMilestonesRequest()
    }

    @Test
    fun shouldThrowIllegalState_IfGetLinkHeader_WhenGettingOpenMilestoneByTitle() {
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

    @Test
    fun shouldCloseMilestone() {
        val closeResponseJson = Fixtures.fixture("github-close-milestone-response.json")

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(closeResponseJson)
                .addGitHubRateLimitHeaders()
        )

        val milestone = milestoneManager.closeMilestone(1)

        assertAll(
            { assertThat(milestone.number).isEqualTo(1) },
            { assertThat(milestone.title).isEqualTo("0.9.0-alpha") },
            {
                assertThat(milestone.htmlUrl)
                    .isEqualTo("https://github.com/sleberknight/kotlin-scratch-pad/milestone/1")
            }
        )

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
