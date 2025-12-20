package org.kiwiproject.changelog

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.kiwiproject.changelog.config.CategoryConfig
import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.github.GitHubChange
import org.kiwiproject.changelog.github.GitHubSearchManager.CommitAuthorsResult
import org.kiwiproject.changelog.github.GitHubUser
import java.time.ZoneOffset
import java.time.ZonedDateTime

@DisplayName("ChangeLogFormatter")
class ChangeLogFormatterKtTest {

    @Nested
    inner class FormatChanges {

        @Test
        fun shouldGenerateEmptyChangelog_WhenThereAreNoChanges() {
            val authors = setOf(
                GitHubUser("dependabot[bot]", "dependabot[bot]", "https://fake-github.com/apps/dependabot")
            )
            val authorsResult = CommitAuthorsResult(authors, 4)
            val changes = listOf<GitHubChange>()
            val repoConfig = repoConfig()
            val categoryConfig = categoryConfig()
            val changelogConfig = changelogConfig(categoryConfig)

            val changelog = formatChangeLog(
                authorsResult,
                changes,
                repoConfig,
                changelogConfig,
                repoConfig.repoUrl()
            )

            assertThat(changelog.trim())
                .isEqualTo(
                    """
                    ## Summary
                    - 2024-07-13T15:44:45Z - [4 commit(s)](https://fake-github.com/fakeorg/fakerepo/compare/v1.4.1...v1.4.2) by [dependabot[bot]](https://fake-github.com/apps/dependabot)

                     - No notable improvements. No issues or pull requests referenced milestone 1.4.2.
                """.trimIndent()
                )
        }

        @ParameterizedTest
        @NullAndEmptySource
        fun shouldGenerateMarkdownFormattedChanges(summary: String?) {
            val authors = gitHubUsers()
            val authorsResult = CommitAuthorsResult(authors, 4)
            val changes = gitHubChanges()
            val repoConfig = repoConfig()
            val categoryConfig = categoryConfig()
            val changelogConfig = changelogConfig(categoryConfig, summary)

            val changelog = formatChangeLog(
                authorsResult,
                changes,
                repoConfig,
                changelogConfig,
                repoConfig.repoUrl()
            )

            assertThat(changelog.trim()).isEqualTo(
                """
                ## Summary
                - 2024-07-13T15:44:45Z - [4 commit(s)](https://fake-github.com/fakeorg/fakerepo/compare/v1.4.1...v1.4.2) by [Scott Leberknight](https://fake-github.com/sleberknight), [dependabot[bot]](https://fake-github.com/apps/dependabot)

                ## Improvements 🚀
                * Make the foo better [(#142)](https://fake-github.com/fakeorg/fakerepo/issues/142)

                ## Bugs 🪲
                * Fix the space modulator [(#154)](https://fake-github.com/fakeorg/fakerepo/issues/154)

                ## Documentation 🗒️
                * Add more documentation to the bar and baz [(#150)](https://fake-github.com/fakeorg/fakerepo/issues/150)

                ## Dependency Updates ⚙️
                * Bump quux-core from 1.6.0 to 1.7.2 [(#151)](https://fake-github.com/fakeorg/fakerepo/pull/151)
            """.trimIndent()
            )
        }

        @Test
        fun shouldGenerateMarkdownFormattedChanges_WithSummary() {
            val authors = gitHubUsers()
            val authorsResult = CommitAuthorsResult(authors, 4)
            val changes = gitHubChanges()
            val repoConfig = repoConfig()
            val categoryConfig = categoryConfig()
            val summary = """
            This is a summary of the release.
            
            It is a _neat_ release with lots of fun _new_ features!
            """.trimIndent()
            val changelogConfig = changelogConfig(categoryConfig, summary)

            val changelog = formatChangeLog(
                authorsResult,
                changes,
                repoConfig,
                changelogConfig,
                repoConfig.repoUrl()
            )

            assertThat(changelog.trim()).isEqualTo(
                """
                ## Summary
                - 2024-07-13T15:44:45Z - [4 commit(s)](https://fake-github.com/fakeorg/fakerepo/compare/v1.4.1...v1.4.2) by [Scott Leberknight](https://fake-github.com/sleberknight), [dependabot[bot]](https://fake-github.com/apps/dependabot)

                This is a summary of the release.
                
                It is a _neat_ release with lots of fun _new_ features!

                ## Improvements 🚀
                * Make the foo better [(#142)](https://fake-github.com/fakeorg/fakerepo/issues/142)

                ## Bugs 🪲
                * Fix the space modulator [(#154)](https://fake-github.com/fakeorg/fakerepo/issues/154)

                ## Documentation 🗒️
                * Add more documentation to the bar and baz [(#150)](https://fake-github.com/fakeorg/fakerepo/issues/150)

                ## Dependency Updates ⚙️
                * Bump quux-core from 1.6.0 to 1.7.2 [(#151)](https://fake-github.com/fakeorg/fakerepo/pull/151)
            """.trimIndent()
            )
        }

        private fun gitHubUsers(): Set<GitHubUser> {
            val authors = setOf(
                GitHubUser("Scott Leberknight", "sleberknight", "https://fake-github.com/sleberknight"),
                GitHubUser("dependabot[bot]", "dependabot[bot]", "https://fake-github.com/apps/dependabot")
            )
            return authors
        }

        private fun gitHubChanges(): List<GitHubChange> {
            val changes = listOf(
                GitHubChange(
                    142,
                    "Make the foo better",
                    "https://fake-github.com/fakeorg/fakerepo/issues/142",
                    "Improvements"
                ),
                GitHubChange(
                    150,
                    "Add more documentation to the bar and baz",
                    "https://fake-github.com/fakeorg/fakerepo/issues/150",
                    "Documentation"
                ),
                GitHubChange(
                    151,
                    "Bump quux-core from 1.6.0 to 1.7.2",
                    "https://fake-github.com/fakeorg/fakerepo/pull/151",
                    "Dependency Updates"
                ),
                GitHubChange(
                    154,
                    "Fix the space modulator",
                    "https://fake-github.com/fakeorg/fakerepo/issues/154",
                    "Bugs"
                )
            )
            return changes
        }

        private fun repoConfig(): RepoConfig {
            val repoConfig = RepoConfig(
                "https://fake-github.com",
                "https://api.fake-github.com",
                "token-12345",
                "fakeorg/fakerepo",
                "v1.4.1",
                "v1.4.2",
                milestone = null
            )
            return repoConfig
        }

        private fun changelogConfig(categoryConfig: CategoryConfig, summary: String? = null) = ChangelogConfig(
            date = ZonedDateTime.of(2024, 7, 13, 15, 44, 45, 34567, ZoneOffset.UTC),
            categoryConfig = categoryConfig,
            summary = summary
        )

        private fun categoryConfig() = CategoryConfig(
            "Assorted",
            mapOf(),
            listOf("Improvements", "Bugs", "Documentation", "Assorted", "Dependency Updates"),
            mapOf(
                "Improvements" to "🚀",
                "Documentation" to "🗒️",
                "Bugs" to "🪲",
                "Dependency Updates" to "⚙️"
            )
        )
    }

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
