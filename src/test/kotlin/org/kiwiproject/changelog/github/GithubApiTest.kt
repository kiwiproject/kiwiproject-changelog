package org.kiwiproject.changelog.github

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.kiwiproject.test.util.Fixtures
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val ISSUES_URL =
    "https://api.github.com/repos/kiwiproject/dropwizard-service-utilities/issues?page=1&per_page=10&state=closed&filter=all&direction=desc"

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

            val issueResponseJson = Fixtures.fixture("github-issues-response.json")
            `when`(httpResponse.body()).thenReturn(issueResponseJson)

            val headerMap = mapOf(
                "Link" to listOf("""<https://api.github.com/repositories/315369011/issues?page=2&per_page=10&state=closed&filter=all&direction=desc>; rel="next", <https://api.github.com/repositories/315369011/issues?page=48&per_page=10&state=closed&filter=all&direction=desc>; rel="last""""),
                "X-RateLimit-Reset" to listOf("1716569454"),
                "X-RateLimit-Remaining" to listOf("59"),
                "X-RateLimit-Limit" to listOf("60"),
            )
            val headers = HttpHeaders.of(headerMap) { _, _ -> true }
            `when`(httpResponse.headers()).thenReturn(headers)

            `when`(httpClient.send(any(HttpRequest::class.java), any(HttpResponse.BodyHandler::class.java)))
                .thenReturn(httpResponse)

            val response = githubApi.get(ISSUES_URL)

            assertThat(response.content).isEqualTo(issueResponseJson)
            assertThat(response.linkHeader).isEqualTo(headerMap["Link"]!!.first())

            val httpRequestCaptor = ArgumentCaptor.forClass(HttpRequest::class.java)

            verify(httpClient).send(httpRequestCaptor.capture(), any(HttpResponse.BodyHandler::class.java))

            val actualHttpRequest = httpRequestCaptor.value
            assertThat(actualHttpRequest.uri()).hasToString(ISSUES_URL)
        }

        @ParameterizedTest
        @ValueSource(ints = [ 100, 201, 202, 204, 300, 301, 400, 500 ])
        fun shouldThrowIoExceptionWhenUnsuccessfulResponse(statusCode: Int) {
            val httpResponse = mock(HttpResponse::class.java)
            `when`(httpResponse.statusCode()).thenReturn(statusCode)

            `when`(httpClient.send(any(HttpRequest::class.java), any(HttpResponse.BodyHandler::class.java)))
                .thenReturn(httpResponse)

            assertThatExceptionOfType(IOException::class.java)
                .isThrownBy { githubApi.get(ISSUES_URL) }
        }
    }

    @Nested
    inner class ResetLimitInLocalTimeOrEmpty {

        @Test
        fun shouldReturnFormattedDateTime_WhenHeaderExists() {
            val resetLimitEpochSeconds = Instant.now().epochSecond

            val responseHeaders = HttpHeaders.of(mapOf(
                "X-RateLimit-Reset" to listOf(resetLimitEpochSeconds.toString())
            )) { _, _ -> true }

            val result = githubApi.resetLimitInLocalTimeOrEmpty(responseHeaders)
            assertThat(result).isEqualTo(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                    Instant.ofEpochSecond(resetLimitEpochSeconds).atZone(ZoneOffset.UTC))
            )
        }

        @Test
        fun shouldReturnEmptyString_WhenHeaderExists() {
            val responseHeaders = HttpHeaders.of(mapOf<String, List<String>>()) { _, _ -> true }

            val result = githubApi.resetLimitInLocalTimeOrEmpty(responseHeaders)
            assertThat(result).isEmpty()
        }
    }
}
