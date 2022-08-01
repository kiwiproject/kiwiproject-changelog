package org.kiwiproject.changelog.github

import org.kiwiproject.io.KiwiIO
import java.io.IOException
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.net.ssl.HttpsURLConnection

class GithubApi(val githubToken: String) {
    fun get(url: String): Response {
        return doRequest(url, "GET")
    }

    private fun doRequest(urlString: String, method: String, body: String? = null) : Response {
        println("Request url: $urlString")
        val url = URL(urlString)

        val connection : HttpsURLConnection = url.openConnection() as HttpsURLConnection
        connection.requestMethod = method
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "token $githubToken")

        if (body != null) {
            connection.outputStream.use { out ->
                out.write(body.toByteArray(StandardCharsets.UTF_8))
                out.flush()
            }
        }

        val rateLimitReset = resetLimitInLocalTimeOrEmpty(connection)
        val rateRemaining = connection.getHeaderField("X-RateLimit-Remaining")
        val rateLimit = connection.getHeaderField("X-RateLimit-Limit")

        println("Github API rate info => Remaining : $rateRemaining, Limit : $rateLimit, Reset at: $rateLimitReset")

        val link = connection.getHeaderField("Link")
        println("Next page 'Link' from Github: $link")

        val content = call(method, connection)
        return Response(content, link)
    }

    private fun resetLimitInLocalTimeOrEmpty(connection: HttpsURLConnection) : String {
        val rateLimitReset : String = connection.getHeaderField("X-RateLimit-Reset") ?: return ""
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(Instant.ofEpochSecond(rateLimitReset.toLong()).atZone(ZoneId.of("UTC")))
    }

    private fun call(method: String, connection: HttpsURLConnection) : String {
        if (connection.responseCode < HTTP_BAD_REQUEST) {
            return KiwiIO.readInputStreamAsString(connection.inputStream)
        }

        val errorBody = KiwiIO.readInputStreamAsString(connection.errorStream)
        throw IOException("$method ${connection.url} failed, response code = ${connection.responseCode}, response body:\n$errorBody")
    }

    class Response(val content: String, val linkHeader: String?)
}
