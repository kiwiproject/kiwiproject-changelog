package org.kiwiproject.changelog.extension

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.kiwiproject.test.okhttp3.mockwebserver.RecordedRequests
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Calls [MockWebServer.url], trims any trailing
 * slashes, and returns the URL as a String.
 */
fun MockWebServer.urlWithoutTrailingSlashAsString(): String =
    urlWithoutTrailingSlashAsString("/")

/**
 * Calls [MockWebServer.url] with the given path, trims any trailing
 * slashes, and returns the URL as a String.
 *
 * @param path the request path
 */
fun MockWebServer.urlWithoutTrailingSlashAsString(path: String): String =
    url(path).toString().trimEnd { it == '/' }

/**
 * Calls [MockWebServer.takeRequest] method with a 1-second timeout.
 * This method expects there to be a request, and asserts that there
 * is a non-null request.
 */
fun MockWebServer.takeRequestWith1SecTimeout(): RecordedRequest =
    this.takeRequest(1, TimeUnit.SECONDS)!!

/**
 * Asserts that there are no more recorded requests for a [MockWebServer].
 */
fun MockWebServer.assertNoMoreRequests() {
    RecordedRequests.assertNoMoreRequests(this)
}

/**
 * Fixed value for the GitHub `X-RateLimit-Limit` header.
 */
const val rateLimitLimit = 5000L

/**
 * A variable for the GitHub `X-RateLimit-Remaining` header.
 * Its value is decremented each time [addGitHubRateLimitHeaders] is called.
 */
private var rateLimitRemaining = 4999L

fun MockResponse.addJsonContentTypeHeader() : MockResponse {
    addHeader("Content-Type", "application/json; charset=utf-8")
    return this
}

/**
 * Decrements `rateLimitRemaining`, and adds the GitHub
 * [rate limit headers](https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api#checking-the-status-of-your-rate-limit).
 *
 * The rate limit reset time is calculated as "now" plus 42 minutes (naturally).
 */
fun MockResponse.addGitHubRateLimitHeaders(): MockResponse {
    rateLimitRemaining--
    return addGitHubRateLimitHeaders(rateLimitRemaining)
}

/**
 * Adds the GitHub
 * [rate limit headers](https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api#checking-the-status-of-your-rate-limit).
 *
 * The rate limit reset time is calculated as "now" plus 42 minutes (naturally).
 */
fun MockResponse.addGitHubRateLimitHeaders(rateLimitRemaining: Long): MockResponse {
    val rateLimitResetAt = Instant.now().plus(42, ChronoUnit.MINUTES).epochSecond

    addHeader("X-RateLimit-Limit", rateLimitLimit)
    addHeader("X-RateLimit-Remaining", rateLimitRemaining)
    addHeader("X-RateLimit-Reset", rateLimitResetAt)
    addHeader("X-RateLimit-Resource", "core")
    return this
}
