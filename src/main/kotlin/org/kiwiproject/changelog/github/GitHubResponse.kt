package org.kiwiproject.changelog.github

import org.kiwiproject.changelog.extension.firstValueAsLongOrThrow
import org.kiwiproject.changelog.extension.firstValueOrNull
import org.kiwiproject.changelog.extension.firstValueOrThrow
import org.kiwiproject.changelog.extension.utcZonedDateTimeFromEpochSeconds
import java.net.URI
import java.net.http.HttpResponse
import java.time.Duration
import java.time.ZonedDateTime

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
    val rateLimitResetAt: Long,
    val rateLimitResource: String
) {

    /**
     * The UTC date/time when the rate limit resets.
     */
    fun resetAt(): ZonedDateTime = utcZonedDateTimeFromEpochSeconds(rateLimitResetAt)

    /**
     * The duration until the rate limit resets.
     */
    fun timeUntilRateLimitResetsFrom(from: ZonedDateTime): Duration = Duration.between(from, resetAt())

    /**
     * There are requests remaining before the rate limit resets.
     */
    fun belowRateLimit(): Boolean = !exceededRateLimit()

    /**
     * There are no more requests remaining before the rate limit resets.
     */
    fun exceededRateLimit(): Boolean = rateLimitRemaining == 0L

    companion object {

        /**
         * Create a new GitHubResponse from the given HttpResponse.
         */
        fun from(httpResponse: HttpResponse<String>): GitHubResponse {
            val responseHeaders = httpResponse.headers()
            val rateLimitLimit = responseHeaders.firstValueAsLongOrThrow("X-RateLimit-Limit")
            val rateLimitRemaining = responseHeaders.firstValueAsLongOrThrow("X-RateLimit-Remaining")
            val rateLimitResetEpochSeconds = responseHeaders.firstValueAsLongOrThrow("X-RateLimit-Reset")
            val rateLimitResource = responseHeaders.firstValueOrThrow("X-RateLimit-Resource")
            val link = responseHeaders.firstValueOrNull("Link")

            return GitHubResponse(
                httpResponse.statusCode(),
                httpResponse.uri(),
                httpResponse.body(),
                link,
                rateLimitLimit,
                rateLimitRemaining,
                rateLimitResetEpochSeconds,
                rateLimitResource
            )
        }
    }
}
