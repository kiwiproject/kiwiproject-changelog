package org.kiwiproject.changelog.config

import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class OutputType {
    CONSOLE, FILE, GITHUB_RELEASE, GITLAB_RELEASE
}

data class ChangelogConfig(val version: String,
                           val date: ZonedDateTime = ZonedDateTime.now(),
                           val outputType: OutputType = OutputType.CONSOLE,
                           val outputFile: File?,
                           val categoryConfig: CategoryConfig) {

    val dateString: String = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(date)
}
