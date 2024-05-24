package org.kiwiproject.changelog.github

import com.google.common.annotations.VisibleForTesting
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class GithubApi(
    private val githubToken: String,
    private val httpClient: HttpClient = HttpClient.newHttpClient()
) {

    // TODO Enhance the Response type:
    //  - Rename to GithubResponse (or GitHubResponse, in which case rename everything with Github to GitHub)
    //  - Add statusCode and don't throw I/O exception when get non-2xx response
    //  - Add other headers to GitHubResponse (rateLimitReset, rateLimitLimit, rateLimitRemaining)

    fun get(url: String): Response {
        println("GET: $url")

        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "token $githubToken")
            .GET()
            .build()
        val httpResponse = httpClient.send(httpRequest, BodyHandlers.ofString())

        val statusCode = httpResponse.statusCode()
        if (statusCode != 200) {
            throw IOException("GET ${httpRequest.uri()} failed, response code $statusCode, response body\n:${httpResponse.body()}")
        }

        val responseHeaders = httpResponse.headers()
        val rateLimitReset = resetLimitInLocalTimeOrEmpty(responseHeaders)
        val rateRemaining = responseHeaders.firstValue("X-RateLimit-Remaining").orElse(null)
        val rateLimit = responseHeaders.firstValue("X-RateLimit-Limit").orElse(null)

        val currentDateTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
            ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS)
        )
        println("GitHub API rate info => Limit : $rateLimit, Remaining : $rateRemaining, Current time: $currentDateTime, Reset at: $rateLimitReset")

        val link = responseHeaders.firstValue("Link").orElse(null)
        println("GitHub 'Link' header: $link")

        return Response(httpResponse.body(), link)
    }

    @VisibleForTesting
    fun resetLimitInLocalTimeOrEmpty(responseHeaders: HttpHeaders): String {
        val rateLimitReset: String = responseHeaders.firstValue("X-RateLimit-Reset").orElse(null) ?: return ""
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
            Instant.ofEpochSecond(rateLimitReset.toLong()).atZone(ZoneId.of("UTC"))
        )
    }

    class Response(val content: String, val linkHeader: String?)
}
