package org.kiwiproject.changelog.github

import com.google.common.annotations.VisibleForTesting
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
import org.kiwiproject.changelog.extension.firstValueOrNull
import org.kiwiproject.changelog.extension.nowUtcTruncatedToSeconds
import org.kiwiproject.time.KiwiDurationFormatters
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.time.format.DateTimeFormatter

private val LOG = KotlinLogging.logger {}

internal const val RATE_LIMIT_REMAINING_WARNING_THRESHOLD = 5L

class GitHubApi(
    private val githubToken: String,
    private val httpClient: HttpClient = HttpClient.newHttpClient()
) {

    /**
     * Generic method to make a GET request to any GitHub REST API endpoint.
     *
     * Throws [IllegalStateException] if the GitHub rate limit is exceeded.
     */
    fun get(url: String): GitHubResponse {
        LOG.debug { "GET: $url" }

        val httpRequest = newRequestBuilder(url).GET().build()
        return sendRequestAndCheckRateLimit(httpRequest)
    }

    /**
     * Generic method to make a POST request to any GitHub REST API endpoint.
     *
     * Throws [IllegalStateException] if the GitHub rate limit is exceeded.
     */
    fun post(url: String, bodyJson: String): GitHubResponse {
        LOG.debug { "POST: $url" }

        val bodyPublisher = BodyPublishers.ofString(bodyJson)
        val httpRequest = newRequestBuilder(url).POST(bodyPublisher).build()
        return sendRequestAndCheckRateLimit(httpRequest)
    }

    /**
     * Generic method to make a PATCH request to any GitHub REST API endpoint.
     *
     * Throws [IllegalStateException] if the GitHub rate limit is exceeded.
     */
    fun patch(url: String, bodyJson: String): GitHubResponse {
        LOG.debug { "PATCH: $url" }

        val bodyPublisher = BodyPublishers.ofString(bodyJson)
        val httpRequest = newRequestBuilder(url).method("PATCH", bodyPublisher).build()
        return sendRequestAndCheckRateLimit(httpRequest)
    }

    private fun newRequestBuilder(url: String): HttpRequest.Builder =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/vnd.github+json")
            .header("Authorization", "token $githubToken")

    private fun sendRequestAndCheckRateLimit(httpRequest: HttpRequest): GitHubResponse {
        val response = sendRequest(httpRequest)

        val now = nowUtcTruncatedToSeconds()
        val timeUntilReset = response.timeUntilRateLimitResetsFrom(now)
        val humanTimeUntilReset = humanTimeUntilReset(timeUntilReset, response.rateLimitRemaining)

        val currentDateTime = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(now)
        val rateLimitReset = response.resetAt()
        val rateLimitLogMessage = "GitHub API rate info => Limit : ${response.rateLimitLimit}," +
                " Remaining : ${response.rateLimitRemaining}," +
                " Current time: ${currentDateTime}," +
                " Reset at: $rateLimitReset, ${humanTimeUntilReset.message}," +
                " Resource: ${response.rateLimitResource}"
        LOG.at(humanTimeUntilReset.logLevel) { this.message = rateLimitLogMessage }

        check(response.belowRateLimit()) {
            IllegalStateException(
                "Rate limit exceeded for resource: ${response.rateLimitResource}." +
                        " No more requests can be made to that resource until $rateLimitReset (${humanTimeUntilReset.message})"
            )
        }

        return response
    }

    private fun sendRequest(httpRequest: HttpRequest): GitHubResponse {
        val httpResponse = httpClient.send(httpRequest, BodyHandlers.ofString())

        val link = httpResponse.headers().firstValueOrNull("Link")
        LOG.debug { "GitHub 'Link' header: $link" }

        return GitHubResponse.from(httpResponse)
    }
}

@VisibleForTesting
internal fun humanTimeUntilReset(timeUntilReset: Duration, rateLimitRemaining: Long): TimeUntilReset =
    when {
        timeUntilReset.isNegative ->
            TimeUntilReset("Time until reset is negative! ($timeUntilReset)", true, Level.WARN)

        else -> TimeUntilReset(
            "Time until reset: ${KiwiDurationFormatters.formatDurationWords(timeUntilReset)}",
            false,
            logLevelForRateLimitRemaining(rateLimitRemaining)
        )
    }

private fun logLevelForRateLimitRemaining(remaining: Long) =
    if (remaining > RATE_LIMIT_REMAINING_WARNING_THRESHOLD) Level.DEBUG else Level.WARN

internal data class TimeUntilReset(val message: String, val isNegative: Boolean, val logLevel: Level)
