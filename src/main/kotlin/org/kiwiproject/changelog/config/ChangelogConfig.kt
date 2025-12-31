package org.kiwiproject.changelog.config

import org.kiwiproject.changelog.extension.nowUtc
import java.io.File
import java.time.ZonedDateTime

enum class OutputType {
    CONSOLE, FILE, GITHUB;

    companion object {
        fun entriesAsString() = entries.toTypedArray().contentToString()
    }
}

data class ChangelogConfig(
    val date: ZonedDateTime = nowUtc(),
    val useTagDateForRelease: Boolean = false,
    val outputType: OutputType = OutputType.CONSOLE,
    val outputFile: File? = null,
    val categoryConfig: CategoryConfig = CategoryConfig.empty(),
    val summary: String? = null
)
