package org.kiwiproject.changelog.config

import org.kiwiproject.changelog.extension.nowUtc
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class OutputType {
    CONSOLE, FILE, GITHUB;

    companion object {
        fun entriesAsString() = entries.toTypedArray().contentToString()
    }
}

data class ChangelogConfig(
    val date: ZonedDateTime = nowUtc(),
    val outputType: OutputType = OutputType.CONSOLE,
    val outputFile: File? = null,
    val categoryConfig: CategoryConfig,
    val summary: String? = null
) {

    val dateString: String = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX").format(date)
}
