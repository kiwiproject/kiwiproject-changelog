package org.kiwiproject.changelog.github

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource
import org.junit.jupiter.params.provider.ValueSource
import org.kiwiproject.changelog.config.RepoHostConfig
import org.kiwiproject.changelog.github.GithubApi.GitHubResponse
import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("GithubListFetcher")
class GithubListFetcherTest {

    private lateinit var fetcher: GithubListFetcher

    @BeforeEach
    fun setUp() {
        fetcher = GithubListFetcher(
            RepoHostConfig(
                "https://github.com",
                "https://api.github.com",
                "12345",
                "kiwiproject/kiwi"
            )
        )
    }

    @Nested
    inner class CheckOkResponse {

        @Test
        fun shouldNotThrowException_WhenStatusCodeIs200() {
            val response = newGitHubResponse(200)

            assertThatCode { fetcher.checkOkResponse(response) }.doesNotThrowAnyException()
        }

        @ParameterizedTest
        @ValueSource(ints = [100, 201, 202, 204, 300, 301, 400, 500])
        fun shouldThrowIllegalStateException_WhenUnsuccessfulResponse(statusCode: Int) {
            val response = newGitHubResponse(statusCode)

            assertThatIllegalStateException()
                .isThrownBy { fetcher.checkOkResponse(response) }
                .withMessage("GET ${response.requestUri} failed, response code ${response.statusCode}, response body\n:${response.content}")
        }

        private fun newGitHubResponse(statusCode: Int) = GitHubResponse(
            statusCode,
            URI.create("https://acme.com:4242/test"),
            "the content",
            null,
            60,
            42,
            Instant.now().plus(30, ChronoUnit.MINUTES).epochSecond
        )
    }

    @Nested
    inner class GetNextPageUrl {

        @Test
        fun shouldReturn_none_WhenLinkHeaderIsNull() {
            val nextPageUrl = fetcher.getNextPageUrl(null)
            assertThat(nextPageUrl).isEqualTo("none")
        }

        // This should never happen, but just in case...
        @Test
        fun shouldReturn_none_WhenLinkHeaderDoesNotContainNextLink() {
            val link = """<https://api.github.com/repositories/315369011/issues?page=5&per_page=100&state=closed&filter=all&direction=desc>; rel="last""""
            val nextPageUrl = fetcher.getNextPageUrl(link)
            assertThat(nextPageUrl).isEqualTo("none")
        }

        @ParameterizedTest
        @CsvFileSource(resources = ["/link-headers.csv"], quoteCharacter = '\'')
        fun shouldReturnNextPageLink(link: String, expectedNextPageUrl: String) {
            val nextPageUrl = fetcher.getNextPageUrl(link)
            assertThat(nextPageUrl).isEqualTo(expectedNextPageUrl)
        }
    }
}
