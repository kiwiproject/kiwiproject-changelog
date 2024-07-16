package org.kiwiproject.changelog

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.kiwiproject.changelog.config.OutputType
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.github.GitHubMilestoneManager
import org.kiwiproject.changelog.github.GitHubMilestoneManager.GitHubMilestone
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.only
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import picocli.CommandLine.TypeConversionException

@DisplayName("App")
class AppTest {

    @Test
    fun shouldExecuteWithMinimumArgs() {
        val (exitCode, app) = App.execute(
            arrayOf(
                "--debug-args",  // prevent execution
                "--repository",
                "kiwiproject/kiwi",
                "--previous-rev",
                "v0.11.0",
                "--revision",
                "v0.12.0",
            )
        )

        assertAll(
            { assertThat(exitCode).isZero() },
            { assertThat(app.debugArgs).isTrue() },
            { assertThat(app.repoHostUrl).isEqualTo("https://github.com") },
            { assertThat(app.repoHostApi).isEqualTo("https://api.github.com") },
            { assertThat(app.token).isNull() },
            { assertThat(app.previousRevision).isEqualTo("v0.11.0") },
            { assertThat(app.revision).isEqualTo("v0.12.0") },
            { assertThat(app.outputType).isEqualTo(OutputType.CONSOLE) },
            { assertThat(app.outputFile).isNull() },
            { assertThat(app.defaultCategory).isNull() },
            { assertThat(app.labelToCategoryMappings).isEmpty() },
            { assertThat(app.categoryOrder).isEmpty() },
            { assertThat(app.categoryToEmojiMappings).isEmpty() },
            { assertThat(app.configFile).isNull() },
            { assertThat(app.ignoreConfigFiles).isFalse() },
            { assertThat(app.closeMilestone).isFalse() },
            { assertThat(app.milestone).isNull() },
            { assertThat(app.createNextMilestone).isNull() },
        )
    }

    @Test
    fun shouldExecuteWithAllShortArgs() {
        val (exitCode, app) = App.execute(
            arrayOf(
                "-d",  // prevent execution
                "-u",
                "https://fake-github.com",
                "-a",
                "https://api.fake-github.com",
                "-t",
                "12345-abc",
                "-r",
                "kiwiproject/kiwi",
                "-p",
                "v0.11.0",
                "-R",
                "v0.12.0",
                "-o",
                "FILE",
                "-f",
                "/tmp/test-changelog.md",
                "-c",
                "Miscellaneous",
                "-m",
                "enhancement:Improvements",
                "-m",
                "bug:Bugs",
                "-m",
                "dependencies:Dependency Updates",
                "-O",
                "Improvements",
                "-O",
                "Bugs",
                "-O",
                "Dependency Updates",
                "-O",
                "Miscellaneous",
                "-e",
                "Improvements:🚀",
                "-e",
                "Bugs:🐞",
                "-e",
                "Dependency Updates:⏫",
                "-e",
                "Miscellaneous:🛒",
                "-n",
                "/home/bob/.kiwi-changelog-defaults.yml",
                "-g",
                "-C",
                "-M",
                "0.12.0",
                "-N",
                "0.13.0"
            )
        )

        assertExecutionWithAllArgs(exitCode, app)
    }

    @Test
    fun shouldExecuteWithAllLongArgs() {
        val (exitCode, app) = App.execute(
            arrayOf(
                "--debug-args",  // prevent execution
                "--repo-host-url",
                "https://fake-github.com",
                "--repo-host-api-url",
                "https://api.fake-github.com",
                "--repo-host-token",
                "12345-abc",
                "--repository",
                "kiwiproject/kiwi",
                "--previous-rev",
                "v0.11.0",
                "--revision",
                "v0.12.0",
                "--output-type",
                "FILE",
                "--output-file",
                "/tmp/test-changelog.md",
                "--default-category",
                "Miscellaneous",
                "--mapping",
                "enhancement:Improvements",
                "--mapping",
                "bug:Bugs",
                "--mapping",
                "dependencies:Dependency Updates",
                "--category-order",
                "Improvements",
                "--category-order",
                "Bugs",
                "--category-order",
                "Dependency Updates",
                "--category-order",
                "Miscellaneous",
                "-e",
                "Improvements:🚀",
                "--emoji-mapping",
                "Bugs:🐞",
                "--emoji-mapping",
                "Dependency Updates:⏫",
                "--emoji-mapping",
                "Miscellaneous:🛒",
                "--config-file",
                "/home/bob/.kiwi-changelog-defaults.yml",
                "--ignore-config-files",
                "--close-milestone",
                "--milestone",
                "0.12.0",
                "--create-next-milestone",
                "0.13.0"
            )
        )

        assertExecutionWithAllArgs(exitCode, app)
    }

    private fun assertExecutionWithAllArgs(exitCode: Int, app: App) {
        assertAll(
            { assertThat(exitCode).isZero() },
            { assertThat(app.debugArgs).isTrue() },
            { assertThat(app.repoHostUrl).isEqualTo("https://fake-github.com") },
            { assertThat(app.repoHostApi).isEqualTo("https://api.fake-github.com") },
            { assertThat(app.token).isEqualTo("12345-abc") },
            { assertThat(app.previousRevision).isEqualTo("v0.11.0") },
            { assertThat(app.revision).isEqualTo("v0.12.0") },
            { assertThat(app.outputType).isEqualTo(OutputType.FILE) },
            { assertThat(app.outputFile).isEqualTo("/tmp/test-changelog.md") },
            { assertThat(app.defaultCategory).isEqualTo("Miscellaneous") },
            {
                assertThat(app.labelToCategoryMappings).containsExactly(
                    "enhancement:Improvements",
                    "bug:Bugs",
                    "dependencies:Dependency Updates"
                )
            },
            {
                assertThat(app.categoryOrder).containsExactly(
                    "Improvements",
                    "Bugs",
                    "Dependency Updates",
                    "Miscellaneous"
                )
            },
            {
                assertThat(app.categoryToEmojiMappings).containsExactly(
                    "Improvements:🚀",
                    "Bugs:🐞",
                    "Dependency Updates:⏫",
                    "Miscellaneous:🛒"
                )
            },
            { assertThat(app.configFile).isEqualTo("/home/bob/.kiwi-changelog-defaults.yml") },
            { assertThat(app.ignoreConfigFiles).isTrue() },
            { assertThat(app.closeMilestone).isTrue() },
            { assertThat(app.milestone).isEqualTo("0.12.0") },
            { assertThat(app.createNextMilestone).isEqualTo("0.13.0") },
        )
    }

    @Test
    fun shouldGetVersion() {
        val versionArray = App.VersionProvider().version
        assertThat(versionArray)
            .describedAs("This should be 'unknown' in unit test environment")
            .contains("[unknown]")
    }

    @ParameterizedTest
    @CsvSource(
        textBlock = """
        console, CONSOLE
        Console, CONSOLE
        CONSOLE, CONSOLE
        file, FILE
        File, FILE
        FILE, FILE
        github, GITHUB
        GitHub, GITHUB
        GITHUB, GITHUB"""
    )
    fun shouldConvertStringsToOutputType(value: String, expected: OutputType) {
        val converter = App.OutputTypeConverter()
        assertThat(converter.convert(value)).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(strings = ["foo", "bar"])
    fun shouldThrowTypeConversionException_WhenGivenInvalidOutputType(value: String) {
        val converter = App.OutputTypeConverter()
        assertThatExceptionOfType(TypeConversionException::class.java)
            .isThrownBy { converter.convert(value) }
    }

    @Nested
    inner class CloseMilestone {

        private val urlPrefix = "https://fake-github.com/acme/space-modulator/milestones/"

        private lateinit var milestoneManager : GitHubMilestoneManager

        @BeforeEach
        fun setUp() {
            milestoneManager = mock(GitHubMilestoneManager::class.java)
        }

        @Test
        fun shouldClose_BasedOnRevision() {
            val number = 1
            val title = "1.4.2"
            val htmlUrl = urlPrefix + number
            val milestone = GitHubMilestone(number, title, htmlUrl)
            `when`(milestoneManager.getOpenMilestoneByTitle(anyString())).thenReturn(milestone)
            `when`(milestoneManager.closeMilestone(anyInt())).thenReturn(milestone)

            val repoConfig = RepoConfig("", "", "", "", "v1.4.1", "v${title}")
            val closedMilestone = App.closeMilestone(
                repoConfig = repoConfig,
                maybeMilestoneTitle = null,
                milestoneManager = milestoneManager
            )

            assertThat(closedMilestone).isSameAs(milestone)

            verifyMilestoneManagerCalls(title, number)
        }

        @Test
        fun shouldClose_UsingExplicitMilestone() {
            val number = 4
            val title = "1.5.0"
            val htmlUrl = urlPrefix + number
            val milestone = GitHubMilestone(number, title, htmlUrl)
            `when`(milestoneManager.getOpenMilestoneByTitle(anyString())).thenReturn(milestone)
            `when`(milestoneManager.closeMilestone(anyInt())).thenReturn(milestone)

            val repoConfig = RepoConfig("", "", "", "", "v1.4.1", "v${title}")
            val closedMilestone = App.closeMilestone(
                repoConfig = repoConfig,
                maybeMilestoneTitle = "1.5.0",
                milestoneManager = milestoneManager
            )

            assertThat(closedMilestone).isSameAs(milestone)

            verifyMilestoneManagerCalls(title, number)
        }

        private fun verifyMilestoneManagerCalls(title: String, number: Int) {
            verify(milestoneManager).getOpenMilestoneByTitle(title)
            verify(milestoneManager).closeMilestone(number)
            verifyNoMoreInteractions(milestoneManager)
        }
    }

    @Nested
    inner class CreateMilestone {

        private val urlPrefix = "https://fake-github.com/acme/space-modulator/milestones/"

        private lateinit var milestoneManager: GitHubMilestoneManager

        @BeforeEach
        fun setUp() {
            milestoneManager = mock(GitHubMilestoneManager::class.java)
        }

        @Test
        fun shouldCreateNewMilestone() {
            val number = 3
            val title = "4.2.0"
            val htmlUrl = urlPrefix + number
            val milestone = GitHubMilestone(number, title, htmlUrl)
            `when`(milestoneManager.getOpenMilestoneByTitleOrNull(anyString())).thenReturn(null)
            `when`(milestoneManager.createMilestone(anyString())).thenReturn(milestone)

            val newMilestone = App.createMilestone(title, milestoneManager)

            assertThat(newMilestone).isSameAs(milestone)

            verify(milestoneManager).getOpenMilestoneByTitleOrNull(title)
            verify(milestoneManager).createMilestone(title)
            verifyNoMoreInteractions(milestoneManager)
        }

        @Test
        fun shouldReturnExistingMilestone_WhenAlreadyExists() {
            val number = 3
            val title = "4.2.0"
            val htmlUrl = urlPrefix + number
            val milestone = GitHubMilestone(number, title, htmlUrl)
            `when`(milestoneManager.getOpenMilestoneByTitleOrNull(anyString())).thenReturn(milestone)

            val existingMilestone = App.createMilestone(title, milestoneManager)

            assertThat(existingMilestone).isSameAs(milestone)

            verify(milestoneManager, only()).getOpenMilestoneByTitleOrNull(title)
        }
    }
}
