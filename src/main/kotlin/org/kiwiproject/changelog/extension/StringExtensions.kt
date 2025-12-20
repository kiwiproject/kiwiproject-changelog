package org.kiwiproject.changelog.extension

fun String.preview(maxChars: Int = 25): String {
    val preview = take(maxChars)
    val ellipsis = if (length > maxChars) "..." else ""
    return "\"$preview$ellipsis\" [$length chars]"
}
