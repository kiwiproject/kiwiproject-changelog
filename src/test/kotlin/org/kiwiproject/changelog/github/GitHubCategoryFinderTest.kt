package org.kiwiproject.changelog.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.kiwiproject.changelog.config.CategoryConfig

@DisplayName("GitHubCategoryFinder")
class GitHubCategoryFinderTest {

    private lateinit var labelToCategoryMapping: Map<String, String>
    private lateinit var categoryConfig: CategoryConfig
    private lateinit var categoryFinder: GitHubCategoryFinder

    @BeforeEach
    fun setUp() {
        val defaultCategory = "Assorted"
        labelToCategoryMapping = mapOf(
            "API Change" to "API Changes",
            "deprecation" to "Deprecations",
            "new feature" to "Improvements",
            "enhancement" to "Improvements",
            "bug" to "Bugs",
            "documentation" to "Documentation",
            "infrastructure" to "Infrastructure",
            "github_actions" to "Infrastructure",
            "code cleanup" to defaultCategory,
            "refactoring" to defaultCategory,
            "dependencies" to "Dependency Updates"
        )
        categoryConfig = CategoryConfig(
            defaultCategory,
            labelToCategoryMapping,
            listOf(),
            mapOf()
        )

        categoryFinder = GitHubCategoryFinder(categoryConfig)
    }

    @Nested
    inner class FindCategory {

        @Test
        fun shouldReturnDefaultCategory_WhenLabelsAreEmpty() {
            assertThat(categoryFinder.findCategory(listOf())).isEqualTo(categoryConfig.defaultCategory)
        }

        @Test
        fun shouldReturnDefaultCategory_WhenLabelIsNotInMappings() {
            assertThat(categoryFinder.findCategory(listOf("foo"))).isEqualTo(categoryConfig.defaultCategory)
        }

        @Test
        fun shouldReturnMatchingCategory() {
            assertAll(
                {
                    assertThat(categoryFinder.findCategory(listOf("enhancement")))
                        .isEqualTo(labelToCategoryMapping["enhancement"])
                },
                {
                    assertThat(categoryFinder.findCategory(listOf("code cleanup")))
                        .isEqualTo(labelToCategoryMapping["code cleanup"])
                },
                {
                    assertThat(categoryFinder.findCategory(listOf("documentation")))
                        .isEqualTo(labelToCategoryMapping["documentation"])
                },
                {
                    assertThat(categoryFinder.findCategory(listOf("dependencies")))
                        .isEqualTo(labelToCategoryMapping["dependencies"])
                },
            )
        }
    }
}
