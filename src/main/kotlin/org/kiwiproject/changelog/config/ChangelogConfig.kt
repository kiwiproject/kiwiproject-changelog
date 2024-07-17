package org.kiwiproject.changelog.config

import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class OutputType {
    CONSOLE, FILE, GITHUB;

    companion object {
        fun entriesAsString() = entries.toTypedArray().contentToString()
    }
}

data class ChangelogConfig(val date: ZonedDateTime = ZonedDateTime.now(),
                           val outputType: OutputType = OutputType.CONSOLE,
                           val outputFile: File? = null,
                           val categoryConfig: CategoryConfig) {

    val dateString: String = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(date)
}
