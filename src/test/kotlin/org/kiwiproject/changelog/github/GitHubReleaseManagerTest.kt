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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.kiwiproject.changelog.MockWebServerExtension
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.extension.addGitHubRateLimitHeaders
import org.kiwiproject.changelog.extension.addJsonContentTypeHeader
import org.kiwiproject.changelog.extension.assertNoMoreRequests
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

        val token = RandomStringUtils.secure().nextAlphanumeric(40)
        val repoConfig = RepoConfig(
            "https://fake-github.com",
            server.urlWithoutTrailingSlashAsString(),
            token,
            "sleberknight/kotlin-scratch-pad",
            "v1.1.0",
            "v1.2.0",
            milestone = null
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

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[]")  // no releases, so no problems
                .addJsonContentTypeHeader()
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

        server.assertNoMoreRequests()
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
            { assertThat(getTagRequest.path).isEqualTo("/repos/sleberknight/kotlin-scratch-pad/git/ref/tags/v0.9.0-alpha") }
        )

        server.assertNoMoreRequests()
    }

    @Test
    fun shouldThrowIllegalState_WhenListReleasesRequestFails() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getTagResponseJson())
                .addJsonContentTypeHeader()
                .addGitHubRateLimitHeaders()
        )

        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{ "error": "oops" }""")
                .addJsonContentTypeHeader()
                .addGitHubRateLimitHeaders()
        )

        assertThatIllegalStateException()
            .isThrownBy { releaseManager.createRelease("v0.9.0-alpha", "The release contents") }
            .withMessage("""List releases was unsuccessful. Status: 500. Text: { "error": "oops" }""")

        assertRequestsWhenReleaseCheckFails()
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
        github-list-releases-response-for-matching-name.json, with name
        github-list-releases-response-for-matching-tag_name.json, associated with tag""")
    fun shouldThrowIllegalState_WhenReleaseAlreadyExists(fixture: String, expectedFailurePhrase: String) {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getTagResponseJson())
                .addJsonContentTypeHeader()
                .addGitHubRateLimitHeaders()
        )

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Fixtures.fixture(fixture))
                .addJsonContentTypeHeader()
                .addGitHubRateLimitHeaders()
        )

        assertThatIllegalStateException()
            .isThrownBy { releaseManager.createRelease("v0.9.0-alpha", "The release contents") }
            .withMessage("A release $expectedFailurePhrase v0.9.0-alpha already exists")

        assertRequestsWhenReleaseCheckFails()
    }

    private fun assertRequestsWhenReleaseCheckFails() {
        val getTagRequest = server.takeRequestWith1SecTimeout()
        val listReleasesRequest = server.takeRequestWith1SecTimeout()

        assertAll(
            { assertThat(getTagRequest.method).isEqualTo("GET") },
            { assertThat(getTagRequest.path).isEqualTo("/repos/sleberknight/kotlin-scratch-pad/git/ref/tags/v0.9.0-alpha") },

            { assertThat(listReleasesRequest.method).isEqualTo("GET") },
            { assertThat(listReleasesRequest.path).isEqualTo("/repos/sleberknight/kotlin-scratch-pad/releases?per_page=100") }
        )

        server.assertNoMoreRequests()
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
                .setResponseCode(200)
                .setBody("[]")
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

        server.assertNoMoreRequests()
    }

    private fun getTagResponseJson(): String = Fixtures.fixture("github-get-tag-response.json")

    private fun getCreateReleaseRequest(): Pair<String, Map<String, Any>> {
        val requestJson = Fixtures.fixture("github-create-release-request.json")
        val requestMap = mapper.readValue<Map<String, Any>>(requestJson)
        return Pair(requestJson, requestMap)
    }

    private fun assertCreateReleaseRequests(requestJson: String) {
        val getTagRequest = server.takeRequestWith1SecTimeout()
        val listReleasesRequest = server.takeRequestWith1SecTimeout()
        val createReleaseRequest = server.takeRequestWith1SecTimeout()

        assertAll(
            { assertThat(getTagRequest.method).isEqualTo("GET") },
            { assertThat(getTagRequest.path).isEqualTo("/repos/sleberknight/kotlin-scratch-pad/git/ref/tags/v0.9.0-alpha") },

            { assertThat(listReleasesRequest.method).isEqualTo("GET") },
            { assertThat(listReleasesRequest.path).isEqualTo("/repos/sleberknight/kotlin-scratch-pad/releases?per_page=100") },

            { assertThat(createReleaseRequest.method).isEqualTo("POST") },
            { assertThat(createReleaseRequest.path).isEqualTo("/repos/sleberknight/kotlin-scratch-pad/releases") },
            { assertThat(createReleaseRequest.body.readUtf8()).isEqualToIgnoringWhitespace(requestJson) }
        )
    }
}
