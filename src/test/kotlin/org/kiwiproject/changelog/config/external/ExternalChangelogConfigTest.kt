package org.kiwiproject.changelog.config.external

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.kiwiproject.test.util.Fixtures
import org.kiwiproject.yaml.YamlHelper

@DisplayName("ExternalChangelogConfig")
class ExternalChangelogConfigTest {

    private val objectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
    private val yamlHelper = YamlHelper(objectMapper)

    @Test
    fun shouldReadConfig() {
        val yaml = Fixtures.fixture("kiwi-changelog-configs/kiwi-changelog.yml")
        val config = readConfig(yaml)

        assertAll(
            {
                assertThat(config.labelCategoryMap()).containsExactlyInAnyOrderEntriesOf(
                    mapOf(
                        "API Change" to "API Changes",
                        "deprecation" to "Deprecations",
                        "new feature" to "Improvements",
                        "enhancement" to "Improvements",
                        "bug" to "Bugs",
                        "documentation" to "Documentation",
                        "infrastructure" to "Infrastructure",
                        "github_actions" to "Infrastructure",
                        "code cleanup" to "Assorted",
                        "refactoring" to "Assorted",
                        "dependencies" to "Dependency Updates"
                    )
                )
            },
            {
                assertThat(config.categoryOrder()).containsExactly(
                    "API Changes",
                    "Deprecations",
                    "Improvements",
                    "Bugs",
                    "Documentation",
                    "Infrastructure",
                    "Assorted",
                    "Dependency Updates"
                )
            },
            { assertThat(config.defaultCategory()).isEqualTo("Assorted") },
            {
                assertThat(config.categoryEmojiMap()).containsExactlyInAnyOrderEntriesOf(
                    mapOf(
                        "API Changes" to "⛔️",
                        "Assorted" to "🛒",
                        "Bugs" to "🪲",
                        "Dependency Updates" to "🆙",
                        "Deprecations" to "⚠️",
                        "Documentation" to "📝",
                        "Improvements" to "🎉",
                        "Infrastructure" to "🏗️"
                    )
                )
            },
            { assertThat(config.stripVPrefixFromNextMilestone).isTrue() },
            { assertThat(config.addVPrefixToRevisions).isFalse() },
            { assertThat(config.useTagDateForRelease).isFalse() }
        )
    }

    @Test
    fun shouldReadConfig_ThatContainsNoDefaultCategory() {
        val yaml = Fixtures.fixture("kiwi-changelog-configs/kiwi-changelog-no-default.yml")
        val config = readConfig(yaml)

        assertAll(
            {
                assertThat(config.labelCategoryMap()).containsExactlyInAnyOrderEntriesOf(
                    mapOf(
                        "new feature" to "Improvements",
                        "enhancement" to "Improvements",
                        "bug" to "Bugs",
                        "infrastructure" to "Infrastructure",
                        "github_actions" to "Infrastructure",
                        "code cleanup" to "Assorted",
                        "refactoring" to "Assorted",
                        "dependencies" to "Dependency Updates"
                    )
                )
            },
            {
                assertThat(config.categoryOrder()).containsExactly(
                    "Improvements",
                    "Bugs",
                    "Infrastructure",
                    "Assorted",
                    "Dependency Updates"
                )
            },
            { assertThat(config.defaultCategory()).isNull() }
        )
    }

    @Test
    fun shouldReadConfig_ThatHasStripVPrefixFromNextMilestone() {
        val yaml = Fixtures.fixture("kiwi-changelog-configs/kiwi-changelog-no-strip-v-prefix.yml")
        val config = readConfig(yaml)

        assertThat(config.stripVPrefixFromNextMilestone).isFalse()
    }

    @Test
    fun shouldReadConfig_ThatHasAddVPrefixToRevisions() {
        val yaml = Fixtures.fixture("kiwi-changelog-configs/kiwi-changelog-add-v-prefix.yml")
        val config = readConfig(yaml)

        assertThat(config.addVPrefixToRevisions).isTrue()
    }

    @Test
    fun shouldReadConfig_ThatHasUseTagDateForRelease() {
        val yaml = Fixtures.fixture("kiwi-changelog-configs/kiwi-changelog-use-tag-date.yml")
        val config = readConfig(yaml)

        assertThat(config.useTagDateForRelease).isTrue()
    }

    @Test
    fun shouldReadEmptyConfig() {
        val yaml = Fixtures.fixture("kiwi-changelog-configs/empty-changelog.yml")
        val config = readConfig(yaml)

        assertAll(
            { assertThat(config.categories).isEmpty() },
            { assertThat(config.labelCategoryMap()).isEmpty() },
            { assertThat(config.categoryOrder()).isEmpty() },
            { assertThat(config.defaultCategory()).isNull() },
            { assertThat(config.stripVPrefixFromNextMilestone).isTrue() },
            { assertThat(config.addVPrefixToRevisions).isFalse() },
            { assertThat(config.useTagDateForRelease).isFalse() }
        )
    }

    private fun readConfig(yaml: String) = yamlHelper.toObject(yaml, ExternalChangelogConfig::class.java)
}
