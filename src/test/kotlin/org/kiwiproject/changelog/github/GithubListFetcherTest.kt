package org.kiwiproject.changelog.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource
import org.kiwiproject.changelog.config.RepoHostConfig

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
