package org.kiwiproject.changelog.extension

import java.net.http.HttpHeaders

fun HttpHeaders.firstValueOrNull(name: String): String? = firstValue(name).orElse(null)

fun HttpHeaders.firstValueOrThrow(name: String): String =
    firstValue(name).orElseThrow { newIllegalStateException(name) }

fun HttpHeaders.firstValueAsLongOrThrow(name: String): Long =
    firstValueAsLong(name).orElseThrow { newIllegalStateException(name) }

private fun newIllegalStateException(name: String): IllegalStateException =
    IllegalStateException("$name header was expected, but does not exist (case-insensitive)")
