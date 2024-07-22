package org.kiwiproject.changelog.extension

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

@DisplayName("DateTimeExtensions")
class DateTimeExtensionsTest {

    @Test
    fun shouldGetCurrentTimeAtUTCTruncatedToSeconds() {
        val now = nowUtcTruncatedToSeconds()

        assertAll(
            { assertThat(now.offset).isEqualTo(ZoneOffset.UTC) },
            { assertThat(now.nano).isZero() }
        )
    }

    @Test
    fun shouldGetCurrentTimeAtUTC() {
        val now = nowUtc()

        assertAll(
            { assertThat(now.offset).isEqualTo(ZoneOffset.UTC) },
            { assertThat(now).isCloseTo(ZonedDateTime.now(ZoneOffset.UTC), within(100, ChronoUnit.MILLIS)) }
        )
    }

    @RepeatedTest(10)
    fun shouldTruncateZonedDateTimeToSeconds() {
        val randomMinutes = Random.nextLong(1, 100)
        val offset = Random.nextInt(10)
        val now = ZonedDateTime.now(ZoneOffset.ofHours(offset)).plusMinutes(randomMinutes)
        val nowWithSecondPrecision = now.truncatedToSeconds()

        assertAll(
            { assertThat(nowWithSecondPrecision.offset).isEqualTo(now.offset) },
            { assertThat(nowWithSecondPrecision.nano).isZero() }
        )
    }

    @RepeatedTest(10)
    fun shouldCreateZonedDateTimeAtUTCFromEpochSeconds() {
        val originalZonedDateTime = ZonedDateTime
            .now(ZoneOffset.ofHours(Random.nextInt(10)))
            .plusMinutes(Random.nextLong(0, 60))
        val epochSeconds = originalZonedDateTime.toEpochSecond()

        val utcZonedDateTime = utcZonedDateTimeFromEpochSeconds(epochSeconds)

        assertThat(utcZonedDateTime)
            .isEqualTo(Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.of("UTC")))
    }
}
