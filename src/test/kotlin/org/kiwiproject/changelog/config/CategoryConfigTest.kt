package org.kiwiproject.changelog.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

@DisplayName("CategoryConfig")
class CategoryConfigTest {

    @Test
    fun shouldCreateEmptyCategoryConfig() {
        val emptyConfig = CategoryConfig.empty()

        assertAll(
            { assertThat(emptyConfig.defaultCategory).isEqualTo("Assorted") },
            { assertThat(emptyConfig.labelToCategoryMapping).isEmpty() },
            { assertThat(emptyConfig.alwaysIncludePRsFrom).isEmpty() },
            { assertThat(emptyConfig.categoryOrder).isEmpty() },
            { assertThat(emptyConfig.categoryToEmoji).isEmpty() }
        )
    }
}
