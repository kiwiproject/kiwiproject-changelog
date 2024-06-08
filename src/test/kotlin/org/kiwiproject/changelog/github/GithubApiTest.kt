package org.kiwiproject.changelog.github

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.kiwiproject.changelog.extension.addGitHubRateLimitHeaders
import org.kiwiproject.changelog.extension.addJsonContentTypeHeader
import org.kiwiproject.changelog.extension.rateLimitLimit
import org.kiwiproject.changelog.extension.takeRequestWith1SecTimeout
import org.kiwiproject.changelog.extension.urlWithoutTrailingSlashAsString
import org.kiwiproject.test.util.Fixtures
import java.net.http.HttpHeaders
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private const val ISSUES_PATH =
    "/repos/kiwiproject/dropwizard-service-utilities/issues?page=1&per_page=10&state=closed&filter=all&direction=desc"

private const val RELEASES_PATH =
    "/repos/sleberknight/kotlin-scratch-pad/releases"

@DisplayName("GithubApi")
class GithubApiTest {

    private lateinit var githubApi: GithubApi
    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()

        val token = RandomStringUtils.randomAlphanumeric(40)
        githubApi = GithubApi(token)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Nested
    inner class Get {

        @Test
        fun shouldGetGithubResource() {
            val issueResponseJson = Fixtures.fixture("github-issues-response.json")
            val link = """<https://api.github.com/repositories/315369011/issues?page=2&per_page=10&state=closed&filter=all&direction=desc>; rel="next", <https://api.github.com/repositories/315369011/issues?page=48&per_page=10&state=closed&filter=all&direction=desc>; rel="last""""

            server.enqueue(MockResponse()
                .setResponseCode(200)
                .setBody(issueResponseJson)
                .addJsonContentTypeHeader()
                .addGitHubRateLimitHeaders()
                .addHeader("Link", link)
            )

            val url = server.urlWithoutTrailingSlashAsString(ISSUES_PATH)
            val response = githubApi.get(url)

            assertAll(
                { assertThat(response.statusCode).isEqualTo(200) },
                { assertThat(response.content).isEqualTo(issueResponseJson) },
                { assertThat(response.linkHeader).isEqualTo(link) },
                { assertThat(response.rateLimitLimit).isEqualTo(rateLimitLimit) },
                { assertThat(response.rateLimitRemaining).isLessThan(response.rateLimitLimit) },
                { assertThat(response.rateLimitResetAt).isGreaterThan(Instant.now().epochSecond) }
            )

            val getRequest = server.takeRequestWith1SecTimeout()
            assertAll(
                { assertThat(getRequest.method).isEqualTo("GET") },
                { assertThat(getRequest.requestUrl).hasToString(url) },
            )
        }
    }

    @Nested
    inner class Post {

        @Test
        fun shouldPostToGitHub() {
            val releaseResponseJson = Fixtures.fixture("github-create-release-response.json")

            server.enqueue(
                MockResponse()
                    .setResponseCode(201)
                    .setBody(releaseResponseJson)
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            val bodyJson = Fixtures.fixture("github-create-release-request.json")
            val url = server.urlWithoutTrailingSlashAsString(RELEASES_PATH)

            val response = githubApi.post(url, bodyJson)

            assertAll(
                { assertThat(response.statusCode).isEqualTo(201) },
                { assertThat(response.content).isEqualTo(releaseResponseJson) },
                { assertThat(response.linkHeader).isNull() },
                { assertThat(response.rateLimitLimit).isEqualTo(5000) },
                { assertThat(response.rateLimitRemaining).isLessThan(response.rateLimitLimit) },
                { assertThat(response.rateLimitResetAt).isGreaterThan(Instant.now().epochSecond) }
            )

            val postRequest = server.takeRequestWith1SecTimeout()
            assertAll(
                { assertThat(postRequest.method).isEqualTo("POST") },
                { assertThat(postRequest.requestUrl).hasToString(url) },
                { assertThat(postRequest.body.readUtf8()).isEqualTo(bodyJson) },
            )
        }
    }

    @Nested
    inner class Patch {

        @Test
        fun shouldSendPatchToGitHub() {
            val closeMilestoneResponseJson = Fixtures.fixture("github-close-milestone-response.json")

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(closeMilestoneResponseJson)
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders()
            )

            val url = server.url("/repos/sleberknight/kotlin-scratch-pad/milestones/1").toString()
            val bodyJson = Fixtures.fixture("github-close-milestone-request.json")

            val response = githubApi.patch(url, bodyJson)

            assertAll(
                { assertThat(response.statusCode).isEqualTo(200) },
                { assertThat(response.content).isEqualTo(closeMilestoneResponseJson) },
                { assertThat(response.linkHeader).isNull() },
                { assertThat(response.rateLimitLimit).isPositive() },
                { assertThat(response.rateLimitRemaining).isLessThan(response.rateLimitLimit) },
                { assertThat(response.rateLimitResetAt).isGreaterThan(Instant.now().epochSecond) },
                {
                    assertThat(response.rateLimitResetAt).isCloseTo(
                        Instant.now().epochSecond,
                        within(TimeUnit.HOURS.toSeconds(1))
                    )
                }
            )

            val patchRequest = server.takeRequestWith1SecTimeout()
            assertAll(
                { assertThat(patchRequest.method).isEqualTo("PATCH") },
                { assertThat(patchRequest.requestUrl).hasToString(url) },
                { assertThat(patchRequest.body.readUtf8()).isEqualTo(bodyJson) },
            )
        }
    }

    @Nested
    inner class ResetLimitAsIsoFormatted {

        @Test
        fun shouldReturnFormattedDateTime_WhenHeaderExists() {
            val resetLimitEpochSeconds = Instant.now().epochSecond

            val responseHeaders = HttpHeaders.of(mapOf(
                "X-RateLimit-Reset" to listOf(resetLimitEpochSeconds.toString())
            )) { _, _ -> true }

            val result = resetLimitAsIsoFormatted(responseHeaders)
            assertThat(result).isEqualTo(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                    Instant.ofEpochSecond(resetLimitEpochSeconds).atZone(ZoneOffset.UTC))
            )
        }

        @Test
        fun shouldThrowIllegalState_WhenHeaderExists() {
            val responseHeaders = HttpHeaders.of(mapOf<String, List<String>>()) { _, _ -> true }

            assertThatIllegalStateException()
                .isThrownBy { resetLimitAsIsoFormatted(responseHeaders) }
        }
    }
}
