package org.kiwiproject.changelog.github

import io.github.oshai.kotlinlogging.Level
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junitpioneer.jupiter.params.LongRangeSource
import org.kiwiproject.changelog.MockWebServerExtension
import org.kiwiproject.changelog.extension.addGitHubRateLimitHeaders
import org.kiwiproject.changelog.extension.addJsonContentTypeHeader
import org.kiwiproject.changelog.extension.assertNoMoreRequests
import org.kiwiproject.changelog.extension.rateLimitLimit
import org.kiwiproject.changelog.extension.takeRequestWith1SecTimeout
import org.kiwiproject.changelog.extension.urlWithoutTrailingSlashAsString
import org.kiwiproject.test.util.Fixtures
import org.kiwiproject.time.KiwiDurationFormatters
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private const val ISSUES_PATH =
    "/repos/kiwiproject/dropwizard-service-utilities/issues?page=1&per_page=10&state=closed&filter=all&direction=desc"

private const val RELEASES_PATH =
    "/repos/sleberknight/kotlin-scratch-pad/releases"

@DisplayName("GitHubApi")
class GitHubApiTest {

    private lateinit var githubApi: GitHubApi
    private lateinit var server: MockWebServer

    @RegisterExtension
    val mockWebServerExtension = MockWebServerExtension()

    @BeforeEach
    fun setUp() {
        server = mockWebServerExtension.server

        val token = RandomStringUtils.secure().nextAlphanumeric(40)
        githubApi = GitHubApi(token)
    }

    @Nested
    inner class Get {

        @Test
        fun shouldGetGitHubResource() {
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

            server.assertNoMoreRequests()
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

            server.assertNoMoreRequests()
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

            server.assertNoMoreRequests()
        }
    }

    @Nested
    inner class RateLimitCheck {

        @Test
        fun shouldThrowIllegalState_WhenRateLimitIsExceeded() {
            val issueResponseJson = Fixtures.fixture("github-issues-response.json")

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(issueResponseJson)
                    .addJsonContentTypeHeader()
                    .addGitHubRateLimitHeaders(rateLimitRemaining = 0)
            )

            val url = server.urlWithoutTrailingSlashAsString(ISSUES_PATH)
            assertThatIllegalStateException()
                .isThrownBy { githubApi.get(url) }
                .withMessageContaining("Rate limit exceeded")

            val getRequest = server.takeRequestWith1SecTimeout()
            assertAll(
                { assertThat(getRequest.method).isEqualTo("GET") },
                { assertThat(getRequest.requestUrl).hasToString(url) },
            )

            server.assertNoMoreRequests()
        }
    }

    @Nested
    inner class HumanTimeUntilReset {

        @ParameterizedTest
        @ValueSource(longs = [0, 5, 10, 60, 180, 86_400])
        fun shouldReturnFormattedDuration_WhenPositiveOrZero(durationSeconds: Long) {
            val duration = Duration.ofSeconds(durationSeconds)
            val rateLimitRemaining = Random.nextLong(RATE_LIMIT_REMAINING_WARNING_THRESHOLD + 1, 30)
            val timeUntilReset = humanTimeUntilReset(duration, rateLimitRemaining)
            assertAll(
                { assertThat(timeUntilReset.isNegative).isFalse() },
                { assertThat(timeUntilReset.logLevel).isEqualTo(Level.DEBUG) },
                {
                    assertThat(timeUntilReset.message)
                        .isEqualTo("Time until reset: ${KiwiDurationFormatters.formatDurationWords(duration)}")
                }
            )
        }

        @ParameterizedTest
        @LongRangeSource(from = RATE_LIMIT_REMAINING_WARNING_THRESHOLD + 1, to = 10, closed = true)
        fun shouldReturnDebugLogLevel_WhenRateLimitRemainingIsAboveWarningThreshold(rateLimitRemaining: Long) {
            val duration = Duration.ofSeconds(45)
            val timeUntilReset = humanTimeUntilReset(duration, rateLimitRemaining)
            assertThat(timeUntilReset.logLevel).isEqualTo(Level.DEBUG)
        }

        @ParameterizedTest
        @LongRangeSource(from = 0, to = RATE_LIMIT_REMAINING_WARNING_THRESHOLD, closed = true)
        fun shouldReturnWarnLogLevel_WhenRateLimitRemainingIsAtOrBelowWarningThreshold(rateLimitRemaining: Long) {
            val duration = Duration.ofSeconds(60)
            val timeUntilReset = humanTimeUntilReset(duration, rateLimitRemaining)
            assertThat(timeUntilReset.logLevel).isEqualTo(Level.WARN)
        }

        @ParameterizedTest
        @ValueSource(longs = [-100, -50, -25, -1])
        fun shouldReturnWarning_WhenNegative(durationSeconds: Long) {
            val duration = Duration.ofSeconds(durationSeconds)
            val rateLimitRemaining = Random.nextLong(10, 30)
            val timeUntilReset = humanTimeUntilReset(duration, rateLimitRemaining)
            assertAll(
                { assertThat(timeUntilReset.isNegative).isTrue() },
                { assertThat(timeUntilReset.logLevel).isEqualTo(Level.WARN) },
                { assertThat(timeUntilReset.message).isEqualTo("Time until reset is negative! ($duration)") }
            )
        }
    }
}
