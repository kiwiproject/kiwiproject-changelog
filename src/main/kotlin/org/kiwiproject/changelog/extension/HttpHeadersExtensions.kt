package org.kiwiproject.changelog.extension

import java.net.http.HttpHeaders

fun HttpHeaders.firstValueOrNull(name: String): String? = firstValue(name).orElse(null)

fun HttpHeaders.firstValueAsLongOrThrow(name: String): Long =
    firstValueAsLong(name).orElseThrow { IllegalStateException("$name header is required")}
