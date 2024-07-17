package org.kiwiproject.changelog.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OutputType")
class OutputTypeTest {

    @Test
    fun shouldReturnAllEnumValues_ForEntriesAsString() {
        val entriesAsString = OutputType.entriesAsString()
        assertThat(entriesAsString)
            .contains("CONSOLE, FILE, GITHUB")
    }
}
