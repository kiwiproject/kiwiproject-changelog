package org.kiwiproject.changelog

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ChangelogGeneratorMain")
class ChangelogGeneratorMainTest {

    @Test
    fun shouldGetVersion() {
        val versionArray = ChangelogGeneratorMain.VersionProvider().version
        assertThat(versionArray)
            .describedAs("This should be 'unknown' in unit test environment")
            .contains("[unknown]")
    }
}
