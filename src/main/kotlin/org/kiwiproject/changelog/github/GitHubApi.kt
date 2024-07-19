package org.kiwiproject.changelog.github

import com.google.common.annotations.VisibleForTesting
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import org.kiwiproject.changelog.extension.firstValueAsLongOrThrow
import org.kiwiproject.changelog.extension.firstValueOrNull
import org.kiwiproject.time.KiwiDurationFormatters
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val LOG = KotlinLogging.logger {}

class GitHubApi(
    private val githubToken: String,
    private val httpClient: HttpClient = HttpClient.newHttpClient()
) {

    /**
     * Generic method to make a GET request to any GitHub REST API endpoint.
     */
    fun get(url: String): GitHubResponse {
        LOG.debug { "GET: $url" }

        val httpRequest = newRequestBuilder(url).GET().build()
        return sendRequest(httpRequest)
    }

    /**
     * Generic method to make a POST request to any GitHub REST API endpoint.
     */
    fun post(url: String, bodyJson: String): GitHubResponse {
        LOG.debug { "POST: $url" }

        val bodyPublisher = BodyPublishers.ofString(bodyJson)
        val httpRequest = newRequestBuilder(url).POST(bodyPublisher).build()
        return sendRequest(httpRequest)
    }

    /**
     * Generic method to make a PATCH request to any GitHub REST API endpoint.
     */
    fun patch(url: String, bodyJson: String): GitHubResponse {
        LOG.debug { "PATCH: $url" }

        val bodyPublisher = BodyPublishers.ofString(bodyJson)
        val httpRequest = newRequestBuilder(url).method("PATCH", bodyPublisher).build()
        return sendRequest(httpRequest)
    }

    private fun newRequestBuilder(url: String): HttpRequest.Builder =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/vnd.github+json")
            .header("Authorization", "token $githubToken")

    private fun sendRequest(httpRequest: HttpRequest): GitHubResponse {
        val httpResponse = httpClient.send(httpRequest, BodyHandlers.ofString())
        return GitHubResponse.from(httpResponse)
    }

    /**
     * Represents a generic GitHub response.
     */
    data class GitHubResponse(
        val statusCode: Int,
        val requestUri: URI,
        val content: String,
        val linkHeader: String?,
        val rateLimitLimit: Long,
        val rateLimitRemaining: Long,
        val rateLimitResetAt: Long
    ) {

        companion object {

            /**
             * Create a new GitHubResponse from the given HttpResponse.
             */
            fun from(httpResponse: HttpResponse<String>): GitHubResponse {
                val responseHeaders = httpResponse.headers()
                val rateLimitLimit = responseHeaders.firstValueAsLongOrThrow("X-RateLimit-Limit")
                val rateLimitRemaining = responseHeaders.firstValueAsLongOrThrow("X-RateLimit-Remaining")
                val rateLimitResetEpochSeconds = responseHeaders.firstValueAsLongOrThrow("X-RateLimit-Reset")

                val now = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS)
                val resetAt = Instant.ofEpochSecond(rateLimitResetEpochSeconds).atZone(ZoneId.of("UTC"))
                val timeUntilReset = Duration.between(now, resetAt)
                val humanTimeUntilReset = humanTimeUntilReset(timeUntilReset)

                val currentDateTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(now)
                val rateLimitReset = epochSecondsAsIsoFormatted(rateLimitResetEpochSeconds)
                val rateLimitLogMessage =
                    "GitHub API rate info => Limit : $rateLimitLimit, Remaining : $rateLimitRemaining, Current time: $currentDateTime, Reset at: $rateLimitReset, $humanTimeUntilReset"
                LOG.at(humanTimeUntilReset.logLevel) { humanTimeUntilReset.message }

                val link = responseHeaders.firstValueOrNull("Link")
                LOG.debug { "GitHub 'Link' header: $link" }

                return GitHubResponse(
                    httpResponse.statusCode(),
                    httpResponse.uri(),
                    httpResponse.body(),
                    link,
                    rateLimitLimit,
                    rateLimitRemaining,
                    rateLimitResetEpochSeconds
                )
            }

            @VisibleForTesting
            internal fun humanTimeUntilReset(timeUntilReset: Duration): TimeUntilReset =
                when {
                    timeUntilReset.isNegative -> TimeUntilReset("Time until reset is negative! ($timeUntilReset)", true, Level.WARN)
                    else -> TimeUntilReset("Time until reset: ${KiwiDurationFormatters.formatDurationWords(timeUntilReset)}", false, Level.DEBUG)
                }

            data class TimeUntilReset(val message: String, val isNegative: Boolean, val logLevel: Level)
        }
    }
}

@VisibleForTesting
fun resetLimitAsIsoFormatted(responseHeaders: HttpHeaders): String {
    val rateLimitReset = responseHeaders.firstValueAsLongOrThrow("X-RateLimit-Reset")
    return epochSecondsAsIsoFormatted(rateLimitReset)
}

private fun epochSecondsAsIsoFormatted(epochSeconds: Long): String {
    return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
        Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.of("UTC"))
    )
}
