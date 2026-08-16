package org.kiwiproject.changelog.extension

import java.net.http.HttpHeaders
import java.util.OptionalLong

/**
 * Thrown when an expected HTTP header is missing, or its value cannot be
 * parsed as the requested type.
 */
class HttpHeaderException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

fun HttpHeaders.firstValueOrNull(name: String): String? = firstValue(name).orElse(null)

/**
 * Throws [HttpHeaderException] if the header does not exist (case-insensitive).
 */
fun HttpHeaders.firstValueOrThrow(name: String): String =
    firstValue(name).orElseThrow { newHttpHeaderException(name) }

/**
 * Throws [HttpHeaderException] if the header does not exist (case-insensitive),
 * or if it exists but its value does not parse as a Long.
 */
fun HttpHeaders.firstValueAsLongOrThrow(name: String): Long {
    val optionalLong: OptionalLong? = try {
        firstValueAsLong(name)
    } catch (e: Exception) {
        throw HttpHeaderException("$name header exists, but its value does not parse as a Long", e)
    }
    return optionalLong!!.orElseThrow { newHttpHeaderException(name) }
}

private fun newHttpHeaderException(name: String): HttpHeaderException =
    HttpHeaderException("$name header was expected, but does not exist (case-insensitive)")
