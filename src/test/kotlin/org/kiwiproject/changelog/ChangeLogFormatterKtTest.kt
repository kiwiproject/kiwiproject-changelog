package org.kiwiproject.changelog

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ChangeLogFormatter")
class ChangeLogFormatterKtTest {

    @Nested
    inner class EnsureAllCategories {

        @Test
        fun shouldRequireAtLeastOneTicketCategory() {
            assertThatIllegalArgumentException()
                .isThrownBy { ensureAllCategories(listOf("Improvements", "Bugs", "Other"), setOf()) }
        }

        @Test
        fun shouldReturnTicketCategories_WhenCategoryOrderIsEmpty() {
            val categoryOrder = listOf<String>()
            val ticketCategories = setOf("Improvements", "Bugs", "Assorted", "Dependency Updates")
            val categories = ensureAllCategories(categoryOrder, ticketCategories)

            assertThat(categories).containsExactly(
                "Improvements",
                "Bugs",
                "Assorted",
                "Dependency Updates"
            )
        }

        @Test
        fun shouldReturnCategoriesInCategoryOrder_WhenTheyContainExactSameCategories() {
            val categoryOrder = listOf("Improvements", "Bugs", "Assorted", "Dependency Updates")
            val ticketCategories = setOf("Assorted", "Improvements", "Dependency Updates", "Bugs")
            val categories = ensureAllCategories(categoryOrder, ticketCategories)

            assertThat(categories).containsExactly(
                "Improvements",
                "Bugs",
                "Assorted",
                "Dependency Updates"
            )
        }

        @Test
        fun shouldReturnCategoryOrder_WhenMoreCategoriesInCategoryOrder() {
            val categoryOrder = listOf(
                "Breaking Changes",
                "Deprecations",
                "Improvements",
                "Bugs",
                "Documentation",
                "Assorted",
                "Dependency Updates"
            )
            val ticketCategories = setOf("Assorted", "Improvements", "Dependency Updates", "Bugs")
            val categories = ensureAllCategories(categoryOrder, ticketCategories)

            assertThat(categories).isEqualTo(categoryOrder)
        }

        @Test
        fun shouldReturnCategoryOrder_WithAnyMissingCategoriesAppendedInAscendingSortOrder() {
            val categoryOrder = listOf("Improvements", "Bugs")
            val ticketCategories = setOf(
                "Documentation",
                "Bugs",
                "Dependency Updates",
                "Improvements",
                "Infrastructure",
                "Assorted",
                "Deprecations"
            )
            val categories = ensureAllCategories(categoryOrder, ticketCategories)

            assertThat(categories).containsExactly(
                "Improvements",
                "Bugs",

                // the rest should be the missing ones, in ascending order
                "Assorted",
                "Dependency Updates",
                "Deprecations",
                "Documentation",
                "Infrastructure",
            )
        }
    }
}
