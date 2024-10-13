package org.kiwiproject.changelog.extension

import java.net.http.HttpHeaders
import java.util.OptionalLong

fun HttpHeaders.firstValueOrNull(name: String): String? = firstValue(name).orElse(null)

fun HttpHeaders.firstValueOrThrow(name: String): String =
    firstValue(name).orElseThrow { newIllegalStateException(name) }

fun HttpHeaders.firstValueAsLongOrThrow(name: String): Long {
    val optionalLong: OptionalLong? = try {
        firstValueAsLong(name)
    } catch (e: Exception) {
        throw IllegalStateException("$name header exists, but its value does not parse as a Long", e)
    }
    return optionalLong!!.orElseThrow { newIllegalStateException(name) }
}

private fun newIllegalStateException(name: String): IllegalStateException =
    IllegalStateException("$name header was expected, but does not exist (case-insensitive)")
