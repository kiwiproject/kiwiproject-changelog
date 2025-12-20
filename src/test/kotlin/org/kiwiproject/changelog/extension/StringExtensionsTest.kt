package org.kiwiproject.changelog.extension

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("StringExtensions")
class StringExtensionsTest {

    @Nested
    inner class Preview {

        @Test
        fun shouldNotAddEllipsisWhenStringIsShorterThanMax() {
            val input = "Short summary"

            val result = input.preview(25)

            assertThat(result)
                .isEqualTo("\"Short summary\" [13 chars]")
        }

        @Test
        fun shouldNotAddEllipsisWhenStringLengthEqualsMax() {
            val input = "a".repeat(25)

            val result = input.preview(25)

            assertThat(result)
                .isEqualTo("\"${"a".repeat(25)}\" [25 chars]")
        }

        @Test
        fun shouldAddEllipsisWhenStringIsLongerThanMax() {
            val input = "This is a fun new release with exciting features"

            val result = input.preview(25)

            assertThat(result)
                .isEqualTo("\"This is a fun new release...\" [48 chars]")
        }

        @Test
        fun shouldRespectCustomMaxChars() {
            val input = "abcdefghijklmnopqrstuvwxyz"

            val result = input.preview(10)

            assertThat(result)
                .isEqualTo("\"abcdefghij...\" [26 chars]")
        }

        @Test
        fun shouldHandleEmptyString() {
            val input = ""

            val result = input.preview(25)

            assertThat(result)
                .isEqualTo("\"\" [0 chars]")
        }
    }
}
