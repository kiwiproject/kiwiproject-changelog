package org.kiwiproject.changelog.extension

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

@DisplayName("MapExtensions")
class MapExtensionsTest {

    @Nested
    inner class DoesNotContainKey {

        @Test
        fun shouldBeOppositeOfContainsKey() {
            val m = mapOf(
                "a" to 1,
                "b" to 2,
                "c" to 3,
                "d" to 4
            )

            assertAll(
                { assertThat(m.doesNotContainKey("a")).isEqualTo(!m.containsKey("a")) },
                { assertThat(m.doesNotContainKey("b")).isEqualTo(!m.containsKey("b")) },
                { assertThat(m.doesNotContainKey("c")).isEqualTo(!m.containsKey("c")) },
                { assertThat(m.doesNotContainKey("d")).isEqualTo(!m.containsKey("d")) },
                { assertThat(m.doesNotContainKey("e")).isEqualTo(!m.containsKey("e")) },
                { assertThat(m.doesNotContainKey("f")).isEqualTo(!m.containsKey("f")) },
            )
        }
    }
}
