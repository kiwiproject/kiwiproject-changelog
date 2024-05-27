package org.kiwiproject.changelog.github

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.kiwiproject.test.util.Fixtures
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val ISSUES_URL =
    "https://api.github.com/repos/kiwiproject/dropwizard-service-utilities/issues?page=1&per_page=10&state=closed&filter=all&direction=desc"

private const val RELEASES_URL =
    "https://api.github.com/repos/sleberknight/kotlin-scratch-pad/releases"

@DisplayName("GithubApi")
class GithubApiTest {

    private lateinit var httpClient: HttpClient
    private lateinit var githubApi: GithubApi

    @BeforeEach
    fun setUp() {
        httpClient = mock(HttpClient::class.java)
        githubApi = GithubApi("12345", httpClient)
    }

    @Nested
    inner class Get {

        @Test
        fun shouldGetGithubResource() {
            val httpResponse = mock(HttpResponse::class.java)

            `when`(httpResponse.statusCode()).thenReturn(200)
            `when`(httpResponse.uri()).thenReturn(URI.create(ISSUES_URL))

            val issueResponseJson = Fixtures.fixture("github-issues-response.json")
            `when`(httpResponse.body()).thenReturn(issueResponseJson)

            val nowAsEpochSeconds = Instant.now().plus(42, ChronoUnit.MINUTES).epochSecond

            val headerMap = mapOf(
                "Link" to listOf("""<https://api.github.com/repositories/315369011/issues?page=2&per_page=10&state=closed&filter=all&direction=desc>; rel="next", <https://api.github.com/repositories/315369011/issues?page=48&per_page=10&state=closed&filter=all&direction=desc>; rel="last""""),
                "X-RateLimit-Reset" to listOf(nowAsEpochSeconds.toString()),
                "X-RateLimit-Remaining" to listOf("59"),
                "X-RateLimit-Limit" to listOf("60"),
            )
            val headers = HttpHeaders.of(headerMap) { _, _ -> true }
            `when`(httpResponse.headers()).thenReturn(headers)

            `when`(httpClient.send(any(HttpRequest::class.java), any(HttpResponse.BodyHandler::class.java)))
                .thenReturn(httpResponse)

            val response = githubApi.get(ISSUES_URL)

            assertAll(
                { assertThat(response.statusCode).isEqualTo(200) },
                { assertThat(response.content).isEqualTo(issueResponseJson) },
                { assertThat(response.linkHeader).isEqualTo(headerMap["Link"]!!.first()) },
                { assertThat(response.rateLimitLimit).isEqualTo(60) },
                { assertThat(response.rateLimitRemaining).isEqualTo(59) },
                { assertThat(response.rateLimitResetAt).isEqualTo(nowAsEpochSeconds) }
            )

            val httpRequestCaptor = ArgumentCaptor.forClass(HttpRequest::class.java)

            verify(httpClient).send(httpRequestCaptor.capture(), any(HttpResponse.BodyHandler::class.java))

            val actualHttpRequest = httpRequestCaptor.value
            assertThat(actualHttpRequest.uri()).hasToString(ISSUES_URL)
        }
    }

    @Nested
    inner class Post {

        @Test
        fun shouldPostToGitHub() {
            val httpResponse = mock(HttpResponse::class.java)

            `when`(httpResponse.statusCode()).thenReturn(201)
            `when`(httpResponse.uri()).thenReturn(URI.create(RELEASES_URL))

            val releaseResponseJson = Fixtures.fixture("github-create-release-response.json")
            `when`(httpResponse.body()).thenReturn(releaseResponseJson)

            val rateLimitResetAt = Instant.now().plus(42, ChronoUnit.MINUTES).epochSecond

            val headerMap = mapOf(
                "X-RateLimit-Reset" to listOf(rateLimitResetAt.toString()),
                "X-RateLimit-Remaining" to listOf("59"),
                "X-RateLimit-Limit" to listOf("60"),
            )
            val headers = HttpHeaders.of(headerMap) { _, _ -> true }
            `when`(httpResponse.headers()).thenReturn(headers)

            `when`(httpClient.send(any(HttpRequest::class.java), any(HttpResponse.BodyHandler::class.java)))
                .thenReturn(httpResponse)

            val bodyJson = Fixtures.fixture("github-create-release-request.json")

            val response = githubApi.post(RELEASES_URL, bodyJson)

            assertAll(
                { assertThat(response.statusCode).isEqualTo(201) },
                { assertThat(response.content).isEqualTo(releaseResponseJson) },
                { assertThat(response.linkHeader).isNull() },
                { assertThat(response.rateLimitLimit).isEqualTo(60) },
                { assertThat(response.rateLimitRemaining).isEqualTo(59) },
                { assertThat(response.rateLimitResetAt).isEqualTo(rateLimitResetAt) }
            )

            val httpRequestCaptor = ArgumentCaptor.forClass(HttpRequest::class.java)

            verify(httpClient).send(httpRequestCaptor.capture(), any(HttpResponse.BodyHandler::class.java))

            val actualHttpRequest = httpRequestCaptor.value
            val bodyPublisher = actualHttpRequest.bodyPublisher().get()
            assertAll(
                { assertThat(actualHttpRequest.uri()).hasToString(RELEASES_URL) },
                { assertThat(bodyPublisher.contentLength()).isEqualTo(bodyJson.length.toLong()) }
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
