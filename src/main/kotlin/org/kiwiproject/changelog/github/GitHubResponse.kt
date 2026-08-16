package org.kiwiproject.changelog.github

import io.github.oshai.kotlinlogging.KotlinLogging
import org.kiwiproject.changelog.extension.HttpHeaderException
import org.kiwiproject.changelog.extension.firstValueAsLongOrThrow
import org.kiwiproject.changelog.extension.firstValueOrNull
import org.kiwiproject.changelog.extension.firstValueOrThrow
import org.kiwiproject.changelog.extension.utcZonedDateTimeFromEpochSeconds
import java.net.URI
import java.net.http.HttpResponse
import java.time.Duration
import java.time.ZonedDateTime

private val LOG = KotlinLogging.logger {}

// GitHub error bodies are typically well under 300 characters, e.g.,
// {"message": "Bad credentials","documentation_url": "..."}. Allow roughly
// 4x that in the exception message, so most real error bodies, including
// larger 422 validation payloads, come through without truncation.
internal const val MAX_ERROR_BODY_LENGTH = 1200

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
         *
         * Throws [IllegalStateException] if the GitHub rate-limit headers cannot be parsed
         * from the response, e.g., because the request failed (they are typically absent on
         * an authentication failure) or an expected header was otherwise missing or malformed.
         */
        fun from(httpResponse: HttpResponse<String>): GitHubResponse {
            val rateLimit = parseRateLimitHeaders(httpResponse)
            val link = httpResponse.headers().firstValueOrNull("Link")

            return GitHubResponse(
                httpResponse.statusCode(),
                httpResponse.uri(),
                httpResponse.body(),
                link,
                rateLimit.limit,
                rateLimit.remaining,
                rateLimit.resetEpochSeconds,
                rateLimit.resource
            )
        }

        private fun parseRateLimitHeaders(httpResponse: HttpResponse<String>): RateLimitHeaders {
            val responseHeaders = httpResponse.headers()
            return try {
                RateLimitHeaders(
                    responseHeaders.firstValueAsLongOrThrow("X-RateLimit-Limit"),
                    responseHeaders.firstValueAsLongOrThrow("X-RateLimit-Remaining"),
                    responseHeaders.firstValueAsLongOrThrow("X-RateLimit-Reset"),
                    responseHeaders.firstValueOrThrow("X-RateLimit-Resource")
                )
            } catch (e: HttpHeaderException) {
                val statusCode = httpResponse.statusCode()
                val uri = httpResponse.uri()
                val body = httpResponse.body()

                val message = if (statusCode in 200..299) {
                    "GitHub API response from $uri was HTTP $statusCode but is missing an expected header:" +
                            " ${e.message}. Body: ${truncateBody(body)}"
                } else {
                    val tokenHint = if (statusCode == 401 || statusCode == 403) {
                        " Verify that the GitHub token is correct, complete, and not expired."
                    } else {
                        ""
                    }
                    "GitHub API request to $uri failed with HTTP $statusCode: ${truncateBody(body)}.$tokenHint"
                }

                LOG.error { "$message (untruncated body: $body)" }
                throw IllegalStateException(message, e)
            }
        }

        private fun truncateBody(body: String): String =
            if (body.length <= MAX_ERROR_BODY_LENGTH) {
                body
            } else {
                "${body.take(MAX_ERROR_BODY_LENGTH)}...(truncated, ${body.length} bytes total)"
            }
    }
}

private data class RateLimitHeaders(
    val limit: Long,
    val remaining: Long,
    val resetEpochSeconds: Long,
    val resource: String
)
