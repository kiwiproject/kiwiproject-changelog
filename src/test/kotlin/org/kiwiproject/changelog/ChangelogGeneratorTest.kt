package org.kiwiproject.changelog

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.kiwiproject.changelog.config.CategoryConfig
import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.OutputType
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.github.*
import org.kiwiproject.changelog.github.GitHubSearchManager.CommitAuthorsResult
import org.kiwiproject.changelog.junit.StdIoExtension
import org.kiwiproject.test.junit.jupiter.ClearBoxTest
import org.kiwiproject.test.util.Fixtures
import org.mockito.Mockito.*
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZoneOffset
import java.time.ZonedDateTime

@DisplayName("ChangelogGenerator")
class ChangelogGeneratorTest {

    @RegisterExtension
    private val stdIo = StdIoExtension()

    @Nested
    inner class Generate {

        @Test
        fun shouldGenerateToConsole() {
            val repoConfig = repoConfig()

            val releaseDateTime = releaseDateTime()
            val categoryConfig = categoryConfig()
            val changelogConfig = ChangelogConfig(
                date = releaseDateTime,
                categoryConfig = categoryConfig
            )

            val releaseManager = mock(GitHubReleaseManager::class.java)
            val searchManager = mock(GitHubSearchManager::class.java)

            val changelogGenerator = ChangelogGenerator(repoConfig, changelogConfig, releaseManager, searchManager)

            val commitAuthorsResult = mockFindUniqueAuthors(searchManager)
            val issues = mockIssueSearch(searchManager, releaseDateTime)

            val result = changelogGenerator.generate()

            val output = stdIo.capturedLines().toList()
            assertAll(
                { assertThat(result.changeCount).isEqualTo(issues.size) },
                { assertThat(result.commitCount).isEqualTo(commitAuthorsResult.totalCommits) },
                { assertThat(result.uniqueAuthorCount).isEqualTo(commitAuthorsResult.authors.size) },
                { assertThat(result.changelogText.trimEnd()).isEqualTo(expectedChangelogContent()) },
                { assertThat(output).contains("## Summary") },
                { assertThat(output).contains("- 2024-07-14T17:29:42Z - [4 commit(s)](https://fake-github.com/kiwiproject/kiwi-test/compare/1.4.1...v1.4.2) by [Alice](https://fake-github.com/bob42), [Bob](https://fake-github.com/bob42), [dependabot[bot]](https://fake-github.com/apps/dependabot)") },
                { assertThat(output).contains("## Enhancements") },
                { assertThat(output).contains("* Add method foobar in BazUtils [(#142)](https://fake-github.com/fakeorg/fakerepo/issues/142)") },
                { assertThat(output).contains("* Add BarFilter to prevent queue saturation [(#101)](https://fake-github.com/fakeorg/fakerepo/issue/101)") },
                { assertThat(output).contains("## Glitches") },
                { assertThat(output).contains("* Fix the stack overflow in BarFoo [(#137)](https://fake-github.com/fakeorg/fakerepo/issue/137)") },
                { assertThat(output).contains("## Everything Else") },
                { assertThat(output).contains("* Bump acme-utils from 4.1.0 to 4.2.0 [(#139)](https://fake-github.com/fakeorg/fakerepo/pull/139)") },
            )

            verify(searchManager).findUniqueAuthorsInCommitsBetween(
                repoConfig.previousRevision,
                repoConfig.revision
            )
            verify(searchManager).findIssuesByMilestone(repoConfig.milestone())
            verifyNoMoreInteractions(searchManager)
            verifyNoInteractions(releaseManager)
        }

        @Test
        fun shouldGenerateToFile(@TempDir dir: Path) {
            val repoConfig = repoConfig()

            val releaseDateTime = releaseDateTime()
            val categoryConfig = categoryConfig()
            val changelogFile = dir.resolve("test-changelog.md").toFile()
            val changelogConfig = ChangelogConfig(
                date = releaseDateTime,
                categoryConfig = categoryConfig,
                outputType = OutputType.FILE,
                outputFile = changelogFile
            )

            val releaseManager = mock(GitHubReleaseManager::class.java)
            val searchManager = mock(GitHubSearchManager::class.java)

            val changelogGenerator = ChangelogGenerator(repoConfig, changelogConfig, releaseManager, searchManager)

            val commitAuthorsResult = mockFindUniqueAuthors(searchManager)
            val issues = mockIssueSearch(searchManager, releaseDateTime)

            val result = changelogGenerator.generate()

            val changelogFileContent = changelogFile.readLines()
                .joinToString(separator = System.lineSeparator())
                .trimEnd()

            val expectedChangelogContent = expectedChangelogContent()

            val output = stdIo.capturedLines()
            assertAll(
                { assertThat(result.changeCount).isEqualTo(issues.size) },
                { assertThat(result.commitCount).isEqualTo(commitAuthorsResult.totalCommits) },
                { assertThat(result.uniqueAuthorCount).isEqualTo(commitAuthorsResult.authors.size) },
                { assertThat(result.changelogText.trimEnd()).isEqualTo(expectedChangelogContent) },
                { assertThat(output).doesNotContain("## Summary") },
                { assertThat(output).doesNotContain("## Enhancements") },
                { assertThat(output).doesNotContain("## Glitches") },
                { assertThat(output).doesNotContain("## Everything Else") },
                { assertThat(output).contains("✅ Wrote changelog to $changelogFile") },
                { assertThat(changelogFileContent).isEqualTo(expectedChangelogContent) }
            )

            verify(searchManager).findUniqueAuthorsInCommitsBetween(
                repoConfig.previousRevision,
                repoConfig.revision
            )
            verify(searchManager).findIssuesByMilestone(repoConfig.milestone())
            verifyNoMoreInteractions(searchManager)
            verifyNoInteractions(releaseManager)
        }

        private fun expectedChangelogContent() =
            Fixtures.fixture("ChangelogGeneratorTest/test-changelog.md").trimEnd()

        private fun releaseDateTime(): ZonedDateTime = ZonedDateTime.of(
            2024, 7, 14, 17, 29, 42, 129, ZoneOffset.UTC
        )

        private fun mockFindUniqueAuthors(searchManager: GitHubSearchManager): CommitAuthorsResult {
            val result = CommitAuthorsResult(
                setOf(
                    GitHubUser("Alice", "alice12", "https://fake-github.com/bob42"),
                    GitHubUser("Bob", "bob42", "https://fake-github.com/bob42"),
                    GitHubUser("dependabot[bot]", "dependabot[bot]", "https://fake-github.com/apps/dependabot")
                ),
                4
            )

            `when`(searchManager.findUniqueAuthorsInCommitsBetween(anyString(), anyString()))
                .thenReturn(result)

            return result
        }

        private fun mockIssueSearch(
            searchManager: GitHubSearchManager,
            releaseDateTime: ZonedDateTime
        ): List<GitHubIssue> {

            val issues = listOf(
                GitHubIssue(
                    "Add method foobar in BazUtils",
                    142,
                    "https://fake-github.com/fakeorg/fakerepo/issues/142",
                    listOf("enhancement"),
                    null,
                    releaseDateTime.minusDays(2)
                ),
                GitHubIssue(
                    "Bump acme-utils from 4.1.0 to 4.2.0",
                    139,
                    "https://fake-github.com/fakeorg/fakerepo/pull/139",
                    listOf("dependencies", "java"),
                    null,
                    releaseDateTime.minusDays(4)
                ),
                GitHubIssue(
                    "Fix the stack overflow in BarFoo",
                    137,
                    "https://fake-github.com/fakeorg/fakerepo/issue/137",
                    listOf("bug"),
                    null,
                    releaseDateTime.minusDays(5)
                ),
                GitHubIssue(
                    "Add BarFilter to prevent queue saturation",
                    101,
                    "https://fake-github.com/fakeorg/fakerepo/issue/101",
                    listOf("new feature"),
                    null,
                    releaseDateTime.minusDays(8)
                )
            )

            `when`(searchManager.findIssuesByMilestone(anyString())).thenReturn(issues)

            return issues
        }

        private fun categoryConfig() = CategoryConfig(
            "Everything Else",
            mapOf(
                "enhancement" to "Enhancements",
                "new feature" to "Enhancements",
                "bug" to "Glitches"
            ),
            listOf("Enhancements", "Glitches", "Everything Else"),
            mapOf()
        )
    }

    @Nested
    inner class WriteChangeLog {

        @Test
        fun shouldWriteToStdOut() {
            val repoConfig = repoConfig()
            val changelogConfig = changelogConfig(OutputType.CONSOLE)
            val releaseManager = mock(GitHubReleaseManager::class.java)
            val searchManager = mock(GitHubSearchManager::class.java)

            val changelogGenerator = ChangelogGenerator(repoConfig, changelogConfig, releaseManager, searchManager)
            val changeLog = changeLogText()
            changelogGenerator.writeChangeLog(changeLog)

            val output = stdIo.capturedLines().toList()
            val expectedStdout = changeLog.split(System.lineSeparator())
            assertThat(output).containsSequence(expectedStdout)

            verifyNoInteractions(releaseManager)
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "parent-dir"])
        fun shouldWriteToFile(parentDir: String, @TempDir directory: Path) {
            val repoConfig = repoConfig()
            val outputFile = Path.of(directory.toString(), parentDir, "changelog.txt")
            val changelogConfig = changelogConfig(OutputType.FILE, outputFile)
            val releaseManager = mock(GitHubReleaseManager::class.java)
            val searchManager = mock(GitHubSearchManager::class.java)

            val changelogGenerator = ChangelogGenerator(repoConfig, changelogConfig, releaseManager, searchManager)
            val changeLog = changeLogText()
            changelogGenerator.writeChangeLog(changeLog)

            val readString = Files.readString(outputFile)
            assertThat(readString).isEqualTo(changeLog)

            verifyNoInteractions(releaseManager)
        }

        @ClearBoxTest
        fun shouldWriteFile_WhenParentFile_IsNull() {
            val repoConfig = repoConfig()
            val outputFile = Path.of("changelog.txt")
            val changelogConfig = changelogConfig(OutputType.FILE, outputFile)
            val releaseManager = mock(GitHubReleaseManager::class.java)
            val searchManager = mock(GitHubSearchManager::class.java)

            val changelogGenerator = ChangelogGenerator(repoConfig, changelogConfig, releaseManager, searchManager)

            try {
                changelogGenerator.writeFile("The changes...", outputFile.toFile())
            } finally {
                Files.delete(outputFile)
            }
            verifyNoInteractions(releaseManager)
        }

        @Test
        fun shouldThrowIllegalArgumentException_WhenNoOutputFile_IsProvided() {
            val repoConfig = repoConfig()
            val changelogConfig = changelogConfig(OutputType.FILE)
            val releaseManager = mock(GitHubReleaseManager::class.java)
            val searchManager = mock(GitHubSearchManager::class.java)

            val changelogGenerator = ChangelogGenerator(repoConfig, changelogConfig, releaseManager, searchManager)

            assertThatIllegalStateException()
                .isThrownBy { changelogGenerator.writeChangeLog(changeLogText()) }
                .withMessage("changeLogConfig.outputFile must not be null. The --output-file (or -f) option is required when output type is file.")

            verifyNoInteractions(releaseManager)
        }

        @Test
        fun shouldWriteToGitHub() {
            val repoConfig = repoConfig()
            val changelogConfig = changelogConfig(OutputType.GITHUB)
            val releaseManager = mock(GitHubReleaseManager::class.java)
            val htmlReleaseUrl =
                "https://fake-github.com/sleberknight/kotlin-scratch-pad/releases/tag/v0.9.0-alpha"
            `when`(releaseManager.createRelease(anyString(), anyString()))
                .thenReturn(GitHubRelease(htmlReleaseUrl))
            val searchManager = mock(GitHubSearchManager::class.java)

            val changelogGenerator = ChangelogGenerator(repoConfig, changelogConfig, releaseManager, searchManager)
            val changeLog = changeLogText()
            changelogGenerator.writeChangeLog(changeLog)

            verify(releaseManager).createRelease(repoConfig.revision, changeLog)
        }
    }

    private fun repoConfig() = RepoConfig(
        url = "https://fake-github.com",
        apiUrl = "https://api.fake-github.com",
        token = "test_token_12345",
        repository = "kiwiproject/kiwi-test",
        previousRevision = "1.4.1",
        revision = "v1.4.2",
        milestone = null
    )

    private fun changelogConfig(outputType: OutputType, outputFile: Path? = null) = ChangelogConfig(
        outputType = outputType,
        outputFile = outputFile?.toFile(),
        categoryConfig = CategoryConfig.empty()
    )

    private fun changeLogText() =
        """
        Improvements
        * Add a new foo to the Bar

        Bugs
        * Fixed the thing
        * Fixed the other thing

        Assorted
        * Improved the README
        """.trimIndent()
}
