package org.kiwiproject.changelog.github

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwiproject.changelog.MockWebServerExtension
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.extension.addGitHubRateLimitHeaders
import org.kiwiproject.changelog.extension.addJsonContentTypeHeader
import org.kiwiproject.changelog.extension.takeRequestWith1MilliTimeout
import org.kiwiproject.changelog.extension.takeRequestWith1SecTimeout
import org.kiwiproject.changelog.extension.urlWithoutTrailingSlashAsString
import org.kiwiproject.test.util.Fixtures

@DisplayName("GitHubReleaseManager")
class GitHubReleaseManagerTest {

    private lateinit var server: MockWebServer
    private lateinit var releaseManager: GitHubReleaseManager
    private val mapper = jacksonObjectMapper()

    @RegisterExtension
    val mockWebServerExtension = MockWebServerExtension()

    @BeforeEach
    fun setUp() {
        server = mockWebServerExtension.server

        val token = RandomStringUtils.randomAlphanumeric(40)
        val repoConfig = RepoConfig(
            "https://fake-github.com",
            server.urlWithoutTrailingSlashAsString(),
            token,
            "sleberknight/kotlin-scratch-pad",
            "v1.1.0",
            "v1.2.0"
        )

        releaseManager = GitHubReleaseManager(repoConfig, GitHubApi(token), mapper)
    }

    @Test
    fun shouldCreateNewRelease() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getTagResponseJson())
                .addGitHubRateLimitHeaders()
        )

        val releaseResponseJson = Fixtures.fixture("github-create-release-response.json")
        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(releaseResponseJson)
                .addJsonContentTypeHeader()
                .addGitHubRateLimitHeaders()
        )

        val (requestJson, requestMap) = getCreateReleaseRequest()
        val tagName = requestMap["tag_name"] as String
        val releaseContent = requestMap["body"] as String

        val release = releaseManager.createRelease(tagName, releaseContent)

        assertThat(release.htmlUrl)
            .isEqualTo("https://fake-github.com/sleberknight/kotlin-scratch-pad/releases/tag/v0.9.0-alpha")

        assertCreateReleaseRequests(requestJson)
    }

    @Test
    fun shouldThrowIllegalState_IfTagDoesNotExist_WhenCreatingRelease() {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("Not Found")
                .addGitHubRateLimitHeaders()
        )

        assertThatIllegalStateException()
            .isThrownBy { releaseManager.createRelease("v0.9.0-alpha", "The release contents") }
            .withMessage("Get tag was unsuccessful. Status: 404. Text: Not Found")

        val getTagRequest = server.takeRequestWith1SecTimeout()
        assertAll(
            { assertThat(getTagRequest.method).isEqualTo("GET") },
            { assertThat(getTagRequest.path).isEqualTo("/repos/sleberknight/kotlin-scratch-pad/git/ref/tags/v0.9.0-alpha") },
            {
                assertThat(server.takeRequestWith1MilliTimeout())
                    .describedAs("The 'create release' request should not have happened")
                    .isNull()
            }
        )
    }

    @Test
    fun shouldThrowIllegalState_IfCreateReleaseFails_WhenCreatingRelease() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getTagResponseJson())
                .addJsonContentTypeHeader()
                .addGitHubRateLimitHeaders()
        )

        server.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setBody("Validation failed")
                .addJsonContentTypeHeader()
                .addGitHubRateLimitHeaders()
        )

        val (requestJson, requestMap) = getCreateReleaseRequest()
        val tagName = requestMap["tag_name"] as String
        val releaseContent = requestMap["body"] as String

        assertThatIllegalStateException()
            .isThrownBy { releaseManager.createRelease(tagName, releaseContent) }
            .withMessage("Create release was unsuccessful. Status: 422. Text: Validation failed")

        assertCreateReleaseRequests(requestJson)
    }

    private fun getTagResponseJson(): String = Fixtures.fixture("github-get-tag-response.json")

    private fun getCreateReleaseRequest(): Pair<String, Map<String, Any>> {
        val requestJson = Fixtures.fixture("github-create-release-request.json")
        val requestMap = mapper.readValue<Map<String, Any>>(requestJson)
        return Pair(requestJson, requestMap)
    }

    private fun assertCreateReleaseRequests(requestJson: String) {
        val getTagRequest = server.takeRequestWith1SecTimeout()
        val createReleaseRequest = server.takeRequestWith1SecTimeout()

        assertAll(
            { assertThat(getTagRequest.method).isEqualTo("GET") },
            { assertThat(getTagRequest.path).isEqualTo("/repos/sleberknight/kotlin-scratch-pad/git/ref/tags/v0.9.0-alpha") },
            { assertThat(createReleaseRequest.method).isEqualTo("POST") },
            { assertThat(createReleaseRequest.path).isEqualTo("/repos/sleberknight/kotlin-scratch-pad/releases") },
            { assertThat(createReleaseRequest.body.readUtf8()).isEqualToIgnoringWhitespace(requestJson) }
        )
    }
}
