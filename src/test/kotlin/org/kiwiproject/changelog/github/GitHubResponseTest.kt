package org.kiwiproject.changelog.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.kiwiproject.changelog.extension.nowUtc
import org.kiwiproject.changelog.extension.truncatedToSeconds
import org.kiwiproject.changelog.extension.utcZonedDateTimeFromEpochSeconds
import java.net.URI
import java.time.Duration
import kotlin.random.Random

@DisplayName("GitHubResponse")
class GitHubResponseTest {

    @RepeatedTest(10)
    fun shouldReturnTheZonedDateTimeWhenRateLimitResetsAtUTC() {
        val randomMinutesFromNow = Random.nextLong(1, 61)
        val rateLimitResetAt = nowUtc().plusMinutes(randomMinutesFromNow)
        val rateLimitResetAtEpochSeconds = rateLimitResetAt.toEpochSecond()

        val response = GitHubResponse(
            200,
            URI.create("https://fake-github.com/some-request"),
            "{}",
            null,
            5000,
            4997,
            rateLimitResetAtEpochSeconds,
            "core"
        )

        val now = nowUtc()
        val duration = response.timeUntilRateLimitResetsFrom(now)

        assertThat(duration).isEqualTo(Duration.between(now, rateLimitResetAt.truncatedToSeconds()))
    }

    @RepeatedTest(10)
    fun shouldCalculateTimeUntilRateLimitResets() {
        val randomMinutesFromNow = Random.nextLong(1, 61)
        val rateLimitResetAt = nowUtc().plusMinutes(randomMinutesFromNow).toEpochSecond()

        val response = GitHubResponse(
            200,
            URI.create("https://fake-github.com/some-request"),
            "{}",
            null,
            5000,
            4997,
            rateLimitResetAt,
            "core"
        )

        assertThat(response.resetAt())
            .isEqualTo(utcZonedDateTimeFromEpochSeconds(rateLimitResetAt))
    }

    @ParameterizedTest
    @CsvSource(
        textBlock = """
        0, true
        1, false
        10, false
        1000, false
        4999, false"""
    )
    fun shouldCheckIfRateLimitHasBeenExceeded(rateLimitRemaining: Long, expectToExceed: Boolean) {
        val response = GitHubResponse(
            200,
            URI.create("https://fake-github.com/some-request"),
            "{}",
            null,
            10,
            rateLimitRemaining,
            nowUtc().plusMinutes(1).toEpochSecond(),
            "core"
        )

        assertAll(
            { assertThat(response.belowRateLimit()).isEqualTo(!expectToExceed) },
            { assertThat(response.exceededRateLimit()).isEqualTo(expectToExceed) },
        )
    }
}
