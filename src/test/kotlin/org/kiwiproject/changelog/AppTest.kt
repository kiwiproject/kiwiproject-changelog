package org.kiwiproject.changelog

import org.assertj.core.api.Assertions.*
import org.assertj.core.api.Assumptions.assumeThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.kiwiproject.changelog.config.OutputType
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.github.GitHubMilestone
import org.kiwiproject.changelog.github.GitHubMilestoneManager
import org.mockito.Mockito.*
import picocli.CommandLine
import picocli.CommandLine.TypeConversionException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

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
                "0.13.0",
                "-s",
                "This is a cool summary of the release!"
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
                "0.13.0",
                "--summary",
                "This is a cool summary of the release!"
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
            { assertThat(app.summary).isEqualTo("This is a cool summary of the release!") },
            { assertThat(app.summaryFile).isNull() },
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["-y", "--summary-file"])
    fun shouldAcceptSummaryFile(summaryArg: String) {
        val (_, app) = App.execute(
            arrayOf(
                "--debug-args",  // prevent execution
                "--repository",
                "kiwiproject/kiwi",
                "--previous-rev",
                "v0.11.0",
                "--revision",
                "v0.12.0",
                summaryArg,
                "/tmp/summary.md"
            )
        )

        assertAll(
            { assertThat(app.summary).isNull() },
            { assertThat(app.summaryFile).isEqualTo("/tmp/summary.md") }
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

        private val url = "https://fake-github.com"
        private val apiUrl = "https://api.fake-github.com"
        private val token = "abc-123"
        private val repository = "fakeorg/fakerepo"
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

            val repoConfig = RepoConfig(url, apiUrl, token, repository, "v1.4.1", "v${title}", milestone = null)
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

            val repoConfig = RepoConfig(url, apiUrl, token, repository, "v1.4.1", "v${title}", milestone = null)
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
        fun `should strip v-prefix from next milestone when enabled`() {
            assertThat(App.resolveNextMilestone("v1.2.3", true)).isEqualTo("1.2.3")
        }

        @Test
        fun `should NOT strip v-prefix from next milestone when disabled`() {
            assertThat(App.resolveNextMilestone("v1.2.3", false)).isEqualTo("v1.2.3")
        }

        @Test
        fun `should NOT strip anything if no v-prefix exists`() {
            assertThat(App.resolveNextMilestone("1.2.3", true)).isEqualTo("1.2.3")
            assertThat(App.resolveNextMilestone("1.2.3", false)).isEqualTo("1.2.3")
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

    @Nested
    inner class ResolveSummary {

        private lateinit var spec: CommandLine.Model.CommandSpec

        @BeforeEach
        fun setUpSpec() {
            spec = CommandLine(App()).commandSpec
        }

        @Test
        fun shouldReturnSummaryWhenSummaryIsProvided() {
            val result = App.resolveSummary(summary = "My summary", summaryFile = null, spec = spec)

            assertThat(result).isEqualTo("My summary")
        }

        @Test
        fun shouldReturnFileContentsWhenSummaryFileIsProvided(@TempDir tempDir: Path) {
            val file = tempDir.resolve("summary.txt").toFile()
            file.writeText("Summary from file")

            val result = App.resolveSummary(summary = null, summaryFile = file.absolutePath, spec = spec)

            assertThat(result).isEqualTo("Summary from file")
        }

        @Test
        fun shouldReturnNullWhenNeitherSummaryNorSummaryFileIsProvided() {
            val result = App.resolveSummary(null, null, spec)

            assertThat(result).isNull()
        }

        @Test
        fun shouldThrowParameterExceptionWhenBothSummaryAndSummaryFileAreProvided(@TempDir tempDir: Path) {
            val file = tempDir.resolve("summary.txt").toFile()
            file.writeText("Summary from file")

            assertThatThrownBy {
                App.resolveSummary(
                    summary = "Inline summary",
                    summaryFile = file.absolutePath,
                    spec = spec
                )
            }.isInstanceOf(CommandLine.ParameterException::class.java)
                .hasMessage("Only one of --summary or --summary-file may be specified")
        }

        @Test
        fun shouldThrowParameterExceptionWhenSummaryFileDoesNotExist() {
            val missingFile = File("does-not-exist.txt").absolutePath

            assertThatThrownBy { App.resolveSummary(summary = null, summaryFile = missingFile, spec = spec) }
                .isInstanceOf(CommandLine.ParameterException::class.java)
                .hasMessage("Summary file does not exist: $missingFile")
        }

        @Test
        fun shouldThrowParameterExceptionWhenSummaryFileIsNotARegularFile(@TempDir tempDir: Path) {
            val file = tempDir.toFile()

            assertThatThrownBy {
                App.resolveSummary(
                    summary = null,
                    summaryFile = file.absolutePath,
                    spec = spec
                )
            }.isInstanceOf(CommandLine.ParameterException::class.java)
                .hasMessage("Summary file is not a regular file: ${file.absolutePath}")
        }

        @Test
        fun shouldThrowParameterExceptionWhenSummaryFileIsNotReadable(@TempDir tempDir: Path) {
            val file = tempDir.resolve("summary.txt")
            file.writeText("Secret summary")

            val supportsPosix = Files.getFileStore(file).supportsFileAttributeView(PosixFileAttributeView::class.java)
            assumeThat(supportsPosix)
                .describedAs("POSIX file permissions not supported on this filesystem")
                .isTrue()

            // Remove read permission (POSIX only)
            Files.setPosixFilePermissions(
                file,
                setOf(PosixFilePermission.OWNER_WRITE)
            )

            val summaryFilePath = file.absolutePathString()
            assertThatThrownBy {
                App.resolveSummary(
                    summary = null,
                    summaryFile = summaryFilePath,
                    spec = spec
                )
            }.isInstanceOf(CommandLine.ParameterException::class.java)
                .hasMessage("Summary file is not readable: $summaryFilePath")
        }
    }
}
