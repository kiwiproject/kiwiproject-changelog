package org.kiwiproject.changelog.extension

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

fun nowUtcTruncatedToSeconds(): ZonedDateTime =
    nowUtc().truncatedToSeconds()

fun nowUtc(): ZonedDateTime =
    ZonedDateTime.now(ZoneOffset.UTC)

fun ZonedDateTime.truncatedToSeconds(): ZonedDateTime =
    truncatedTo(ChronoUnit.SECONDS)

fun utcZonedDateTimeFromEpochSeconds(epochSeconds: Long): ZonedDateTime =
    Instant.ofEpochSecond(epochSeconds).atZone(ZoneOffset.UTC)
