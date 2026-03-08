package org.kiwiproject.changelog.config

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwiproject.changelog.config.external.ExternalCategory
import org.kiwiproject.changelog.config.external.ExternalChangelogConfig
import org.kiwiproject.changelog.junit.StdIoExtension
import org.kiwiproject.test.util.Fixtures.fixture
import org.kiwiproject.test.util.Fixtures.fixturePath
import org.kiwiproject.yaml.RuntimeYamlException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectory

@DisplayName("ConfigHelpers")
class ConfigHelpersTest {

    @Nested
    inner class MappingsToMap {

        @Test
        fun shouldReturnEmptyMap_WhenNoMappingsGiven() {
            val result = ConfigHelpers.mappingsToMap(listOf())
            assertThat(result).isEmpty()
        }

        @Test
        fun shouldSplitMappingArguments() {
            val mappings = listOf(
                "new feature:Improvements",
                "enhancement:Improvements",
                "code cleanup:Assorted",
                "refactoring:Assorted",
                "infrastructure:Infrastructure",
                "dependencies:Dependency Updates"
            )

            val result = ConfigHelpers.mappingsToMap(mappings)
            assertThat(result).containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    "new feature" to "Improvements",
                    "enhancement" to "Improvements",
                    "code cleanup" to "Assorted",
                    "refactoring" to "Assorted",
                    "infrastructure" to "Infrastructure",
                    "dependencies" to "Dependency Updates"
                )
            )
        }
    }

    @Nested
    inner class ExternalConfig {

        private lateinit var parent: File
        private lateinit var current: File
        private lateinit var userHomeDir: File

        @BeforeEach
        fun setUp(@TempDir dir: Path) {
            this.parent = dir.toFile()

            val parentDirAbsPath = dir.toAbsolutePath().toString()
            this.current = Paths.get(parentDirAbsPath, "current").createDirectory().toFile()
            this.userHomeDir = Paths.get(parentDirAbsPath, "home").createDirectory().toFile()
        }

        @Test
        fun shouldReturnEmptyConfigWhenNoConfigFilesExist() {
            val externalConfig = ConfigHelpers.externalConfig(current, userHomeDir, null, false)

            assertThat(externalConfig.categories).isEmpty()
        }

        @Test
        fun shouldIgnoreConfigFilesWhenInstructed() {
            val externalConfig = ConfigHelpers.externalConfig(current, userHomeDir, null, true)

            assertThat(externalConfig.categories).isEmpty()
        }

        @Test
        fun shouldUseExplicitConfigFile() {
            val changelogPath = fixturePath("kiwi-changelog-configs/kiwi-changelog.yml")

            val externalConfig = ConfigHelpers.externalConfig(
                current,
                userHomeDir,
                changelogPath.absolutePathString(),
                false
            )

            assertConfig(externalConfig)
        }

        @Test
        fun shouldReadConfigFromCurrentDirectory() {
            val configFilePath = Path.of(current.absolutePath, ".kiwi-changelog.yml")
            assertReadsConfigFromPath(configFilePath)
        }

        @Test
        fun shouldReadConfigFromParentDirectory() {
            val configFilePath = Path.of(parent.absolutePath, ".kiwi-changelog.yml")
            assertReadsConfigFromPath(configFilePath)
        }

        @Test
        fun shouldReadConfigFromUserHomeDirectory() {
            val configFilePath = Path.of(userHomeDir.absolutePath, ".kiwi-changelog.yml")
            assertReadsConfigFromPath(configFilePath)
        }

        private fun assertReadsConfigFromPath(configFilePath: Path) {
            val yaml = fixture("kiwi-changelog-configs/kiwi-changelog.yml")
            Files.writeString(configFilePath, yaml)

            val externalConfig = ConfigHelpers.externalConfig(current, userHomeDir, null, false)

            assertConfig(externalConfig)
        }

        private fun assertConfig(externalConfig: ExternalChangelogConfig) {
            assertThat(externalConfig.categoryOrder()).containsExactly(
                "API Changes",
                "Deprecations",
                "Improvements",
                "Bugs",
                "Documentation",
                "Infrastructure",
                "Assorted",
                "Dependency Updates"
            )
        }
    }

    @Nested
    inner class ExternalConfigFromYaml {

        @RegisterExtension
        private val stdIo = StdIoExtension()

        @Test
        fun shouldPrintWarning_WhenConfigContainsUnknownProperty() {
            val yaml = """
                ---
                unknownProperty: true
            """.trimIndent()

            ConfigHelpers.externalConfig(yaml)

            assertThat(stdIo.capturedLines()).anyMatch { it.contains("Unknown property 'unknownProperty' in configuration file") }
        }

        @Test
        fun shouldPrintWarningAndRethrow_WhenConfigContainsInvalidTypeForProperty() {
            val yaml = """
                ---
                hyperlinks: not-a-boolean
            """.trimIndent()

            assertThatThrownBy { ConfigHelpers.externalConfig(yaml) }
                .isInstanceOf(RuntimeYamlException::class.java)
                .hasCauseInstanceOf(InvalidFormatException::class.java)

            assertThat(stdIo.capturedLines()).anyMatch { it.contains("Invalid value in configuration file") }
        }

        @Test
        fun shouldRethrowWithoutPrinting_WhenCauseIsNotInvalidFormatException() {
            // "@" is a reserved character in YAML and causes a JsonParseException, not an InvalidFormatException
            val yaml = "---\n@invalid"

            val thrown = catchThrowable { ConfigHelpers.externalConfig(yaml) }
            assertThat(thrown).isInstanceOf(RuntimeYamlException::class.java)
            assertThat(thrown.cause).isNotInstanceOf(InvalidFormatException::class.java)
            assertThat(stdIo.capturedLines()).noneMatch { it.contains("Invalid value in configuration file") }
        }
    }

    @Nested
    inner class InvalidFormatMessage {

        @Test
        fun shouldIncludeLineAndColumn_WhenLocationIsPresent() {
            val yaml = "---\nhyperlinks: not-a-boolean"
            val cause = runCatching { ConfigHelpers.externalConfig(yaml) }
                .exceptionOrNull()
                ?.cause as? InvalidFormatException
                ?: error("Expected RuntimeYamlException with InvalidFormatException cause")

            val message = ConfigHelpers.invalidFormatMessage(cause)

            assertThat(message).contains("Invalid value in configuration file (line")
            assertThat(message).contains("column")
        }

        @Test
        fun shouldNotIncludeLineAndColumn_WhenLocationIsNull() {
            val exception = InvalidFormatException.from(null, "test message", "bad-value", Boolean::class.java)

            val message = ConfigHelpers.invalidFormatMessage(exception)

            assertThat(message).isEqualTo("Invalid value in configuration file: test message")
        }
    }

    @Nested
    inner class BuildCategoryConfig {

        @Test
        fun shouldBuildEmptyConfig() {
            val config = ConfigHelpers.buildCategoryConfig(
                listOf(),
                listOf(),
                listOf(),
                null,
                ExternalChangelogConfig()
            )

            assertAll(
                { assertThat(config.labelToCategoryMapping).isEmpty() },
                { assertThat(config.categoryToEmoji).isEmpty() },
                { assertThat(config.categoryOrder).isEmpty() },
                { assertThat(config.defaultCategory).isEqualTo("Assorted") },
            )
        }

        @Test
        fun shouldBuildConfigFromOnlyArguments() {
            val config = ConfigHelpers.buildCategoryConfig(
                listOf("enhancement:Improvements", "bug:Bugs", "dependencies:Dependency Updates"),
                listOf("Improvements:🎉", "Bugs:🐞"),
                listOf("Improvements", "Bugs", "Random Things", "Dependency Updates"),
                "Random Things",
                ExternalChangelogConfig()
            )

            assertAll(
                {
                    assertThat(config.labelToCategoryMapping).containsExactlyInAnyOrderEntriesOf(
                        mapOf(
                            "bug" to "Bugs",
                            "dependencies" to "Dependency Updates",
                            "enhancement" to "Improvements",
                        )
                    )
                },
                {
                    assertThat(config.categoryToEmoji).containsExactlyInAnyOrderEntriesOf(
                        mapOf(
                            "Bugs" to "🐞",
                            "Improvements" to "🎉"
                        )
                    )
                },
                {
                    assertThat(config.categoryOrder).containsExactly(
                        "Improvements",
                        "Bugs",
                        "Random Things",
                        "Dependency Updates"
                    )
                },
                { assertThat(config.defaultCategory).isEqualTo("Random Things") },
            )
        }

        @Test
        fun shouldBuildConfigFromOnlyExternalConfig() {
            val externalConfig = ExternalChangelogConfig(
                listOf(
                    ExternalCategory("Improvements", "🚀", listOf("enhancement", "new feature"), false),
                    ExternalCategory("Bugs", "🪲", listOf("bug"), false),
                    ExternalCategory("Dependency Updates", "⬆️", listOf("dependencies"), false),
                    ExternalCategory("Documentation", "📄", listOf("javadoc", "documentation"), false),
                    ExternalCategory("Other Changes", "❓", listOf("refactoring", "code cleanup"), true),
                )
            )

            val config = ConfigHelpers.buildCategoryConfig(
                listOf(),
                listOf(),
                listOf(),
                null,
                externalConfig
            )

            assertAll(
                {
                    assertThat(config.labelToCategoryMapping).containsExactlyInAnyOrderEntriesOf(
                        mapOf(
                            "bug" to "Bugs",
                            "dependencies" to "Dependency Updates",
                            "enhancement" to "Improvements",
                            "new feature" to "Improvements",
                            "documentation" to "Documentation",
                            "javadoc" to "Documentation",
                            "refactoring" to "Other Changes",
                            "code cleanup" to "Other Changes",
                        )
                    )
                },
                {
                    assertThat(config.categoryToEmoji).containsExactlyInAnyOrderEntriesOf(
                        mapOf(
                            "Bugs" to "🪲",
                            "Dependency Updates" to "⬆️",
                            "Documentation" to "📄",
                            "Improvements" to "🚀",
                            "Other Changes" to "❓"
                        )
                    )
                },
                {
                    assertThat(config.categoryOrder).containsExactly(
                        "Improvements",
                        "Bugs",
                        "Dependency Updates",
                        "Documentation",
                        "Other Changes",
                    )
                },
                { assertThat(config.defaultCategory).isEqualTo("Other Changes") },
            )
        }

        @Test
        fun shouldPreferArgumentsToExternalConfig() {
            val externalConfig = ExternalChangelogConfig(
                listOf(
                    ExternalCategory("Improvements", "🚀", listOf("enhancement", "new feature"), false),
                    ExternalCategory("Bugs", "🪲", listOf("bug"), false),
                    ExternalCategory("Assorted", "👜", listOf("refactoring", "code cleanup"), false),
                    ExternalCategory("Dependency Updates", "⬆️", listOf("dependencies"), false),
                    ExternalCategory("Other Changes", "👍", listOf("other", "random"), true)
                )
            )

           val labelCategoryMappings = listOf(
               "API change:Breaking Changes",
               "deprecation:Deprecations"
           )
           val categoryEmojiMappings = listOf(
               "Breaking Changes:⛔️",
               "Deprecations:⚠️",
               "Improvements:🎉",
               "Bugs:🪳",
               "Assorted:🛒"
           )
           val categoryOrder = listOf(
               "Breaking Changes",
               "Deprecations",
               "Bugs",
               "Improvements",
               "Assorted",
               "Other Changes",
               "Dependency Updates"
           )

            val config = ConfigHelpers.buildCategoryConfig(
                labelCategoryMappings,
                categoryEmojiMappings,
                categoryOrder,
                "Assorted",
                externalConfig
            )

            assertAll(
                {
                    assertThat(config.labelToCategoryMapping).containsExactlyInAnyOrderEntriesOf(
                        mapOf(
                            "API change" to "Breaking Changes",
                            "deprecation" to "Deprecations",
                            "bug" to "Bugs",
                            "dependencies" to "Dependency Updates",
                            "enhancement" to "Improvements",
                            "new feature" to "Improvements",
                            "code cleanup" to "Assorted",
                            "refactoring" to "Assorted",
                            "other" to "Other Changes",
                            "random" to "Other Changes",
                        )
                    )
                },
                {
                    assertThat(config.categoryToEmoji).containsExactlyInAnyOrderEntriesOf(
                        mapOf(
                            "Breaking Changes" to "⛔️",
                            "Deprecations" to "⚠️",
                            "Bugs" to "🪳",
                            "Improvements" to "🎉",
                            "Other Changes" to "👍",
                            "Assorted" to "🛒",
                            "Dependency Updates" to "⬆️",
                        )
                    )
                },
                {
                    assertThat(config.categoryOrder).containsExactly(
                        "Breaking Changes",
                        "Deprecations",
                        "Bugs",
                        "Improvements",
                        "Assorted",
                        "Other Changes",
                        "Dependency Updates",
                    )
                },
                { assertThat(config.defaultCategory).isEqualTo("Assorted") },
            )
        }
    }
}
