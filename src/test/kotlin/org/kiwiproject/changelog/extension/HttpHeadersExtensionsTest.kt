package org.kiwiproject.changelog.extension

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.http.HttpHeaders

@DisplayName("HttpHeadersExtensions")
class HttpHeadersExtensionsTest {

    private lateinit var httpHeaders : HttpHeaders

    @BeforeEach
    fun setUp() {
        val headerMap = mapOf(
            "Content-Type" to listOf("application/vnd.github+json"),
            "X-RateLimit-Limit" to listOf("5000"),
            "X-RateLimit-Remaining" to listOf("4997"),
            "X-RateLimit-Reset" to listOf("1729522050"),
            "X-RateLimit-Resource" to listOf("core")
        )
        httpHeaders = HttpHeaders.of(headerMap) { name, value -> true}
    }

    @Nested
    inner class FirstValueOrNull {

        @Test
        fun shouldReturnFirstValue_WhenHeaderExists() {
            assertThat(httpHeaders.firstValueOrNull("X-RateLimit-Resource"))
                .isEqualTo("core")
        }

        @Test
        fun shouldReturnNull_WhenHeaderDoesNotExist() {
            assertThat(httpHeaders.firstValueOrNull("Link")).isNull()
        }
    }

    @Nested
    inner class FirstValueOrThrow {

        @Test
        fun shouldReturnFirstValue_WhenHeaderExists() {
            assertThat(httpHeaders.firstValueOrNull("Content-Type"))
                .isEqualTo("application/vnd.github+json")
        }

        @Test
        fun shouldThrowIllegalState_WhenHeaderDoesNotExist() {
            assertThatIllegalStateException()
                .isThrownBy { httpHeaders.firstValueOrThrow("Link") }
                .withMessage("Link header was expected, but does not exist (case-insensitive)")
        }
    }

    @Nested
    inner class FirstValueAsLongOrThrow {

        @Test
        fun shouldReturnFirstValue_ConvertedToLong_WhenHeaderExists() {
            assertThat(httpHeaders.firstValueAsLongOrThrow("X-RateLimit-Limit"))
                .isEqualTo(5000L)
        }

        @Test
        fun shouldThrowIllegalState_WhenHeaderDoesNotExist() {
            assertThatIllegalStateException()
                .isThrownBy { httpHeaders.firstValueAsLongOrThrow("Some-Custom-Header") }
                .withMessage("Some-Custom-Header header was expected, but does not exist (case-insensitive)")
        }

        @Test
        fun shouldThrowIllegalState_WhenHeaderExists_ButValueIsNotConvertibleToLong() {
            assertThatIllegalStateException()
                .isThrownBy { httpHeaders.firstValueAsLongOrThrow("X-RateLimit-Resource") }
                .withMessage("X-RateLimit-Resource header exists, but its value does not parse as a Long")
                .havingCause()
                .isExactlyInstanceOf(NumberFormatException::class.java)
        }
    }
}
