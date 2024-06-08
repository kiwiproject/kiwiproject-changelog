package org.kiwiproject.changelog

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junitpioneer.jupiter.StdIo
import org.junitpioneer.jupiter.StdOut
import org.kiwiproject.changelog.config.CategoryConfig
import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.OutputType
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.config.RepoHostConfig
import org.kiwiproject.changelog.github.GitHubReleaseManager
import org.kiwiproject.changelog.github.GitHubReleaseManager.GitHubRelease
import org.kiwiproject.test.junit.jupiter.ClearBoxTest
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@DisplayName("GenerateChangelog")
class GenerateChangelogTest {

    @Nested
    inner class WriteChangeLog {

        @Test
        @StdIo
        fun shouldWriteToStdOut(stdout: StdOut) {
            val repoHostConfig = repoHostConfig()
            val repoConfig = repoConfig()
            val changelogConfig = changelogConfig(OutputType.CONSOLE)
            val releaseManager = mock(GitHubReleaseManager::class.java)

            val generateChangelog = GenerateChangelog(repoHostConfig, repoConfig, changelogConfig, releaseManager)
            val changeLog = changeLogText()
            generateChangelog.writeChangeLog(changeLog)

            val output = stdout.capturedLines().toList()
            val expectedStdout = changeLog.split(System.lineSeparator())
            assertThat(output).containsSequence(expectedStdout)

            verifyNoInteractions(releaseManager)
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "parent-dir"])
        fun shouldWriteToFile(parentDir: String, @TempDir directory: Path) {
            val repoHostConfig = repoHostConfig()
            val repoConfig = repoConfig()
            val outputFile = Path.of(directory.toString(), parentDir, "changelog.txt")
            val changelogConfig = changelogConfig(OutputType.FILE, outputFile)
            val releaseManager = mock(GitHubReleaseManager::class.java)

            val generateChangelog = GenerateChangelog(repoHostConfig, repoConfig, changelogConfig, releaseManager)
            val changeLog = changeLogText()
            generateChangelog.writeChangeLog(changeLog)

            val readString = Files.readString(outputFile)
            assertThat(readString).isEqualTo(changeLog)

            verifyNoInteractions(releaseManager)
        }

        @ClearBoxTest
        fun shouldWriteFile_WhenParentFile_IsNull() {
            val repoHostConfig = repoHostConfig()
            val repoConfig = repoConfig()
            val outputFile = Path.of("changelog.txt")
            val changelogConfig = changelogConfig(OutputType.FILE, outputFile)
            val releaseManager = mock(GitHubReleaseManager::class.java)

            val generateChangelog = GenerateChangelog(repoHostConfig, repoConfig, changelogConfig, releaseManager)

            try {
                generateChangelog.writeFile("The changes...", outputFile.toFile())
            } finally {
                Files.delete(outputFile)
            }
            verifyNoInteractions(releaseManager)
        }

        @Test
        fun shouldThrowIllegalArgumentException_WhenNoOutputFile_IsProvided() {
            val repoHostConfig = repoHostConfig()
            val repoConfig = repoConfig()
            val changelogConfig = changelogConfig(OutputType.FILE)
            val releaseManager = mock(GitHubReleaseManager::class.java)

            val generateChangelog = GenerateChangelog(repoHostConfig, repoConfig, changelogConfig, releaseManager)

            assertThatIllegalStateException()
                .isThrownBy { generateChangelog.writeChangeLog(changeLogText()) }
                .withMessage("changeLogConfig.outputFile must not be null. The --output-file (or -f) option is required when output type is file.")

            verifyNoInteractions(releaseManager)
        }

        @Test
        fun shouldWriteToGitHub() {
            val repoHostConfig = repoHostConfig()
            val repoConfig = repoConfig()
            val changelogConfig = changelogConfig(OutputType.GITHUB)
            val releaseManager = mock(GitHubReleaseManager::class.java)
            val htmlReleaseUrl =
                "https://github.com/sleberknight/kotlin-scratch-pad/releases/tag/v0.9.0-alpha"
            `when`(releaseManager.createRelease(anyString(), anyString()))
                .thenReturn(GitHubRelease(htmlReleaseUrl))

            val generateChangelog = GenerateChangelog(repoHostConfig, repoConfig, changelogConfig, releaseManager)
            val changeLog = changeLogText()
            generateChangelog.writeChangeLog(changeLog)

            verify(releaseManager).createRelease(repoConfig.revision, changeLog)
        }
    }

    private fun repoHostConfig() = RepoHostConfig(
        url = "https://github.com",
        apiUrl = "https://api.github.com",
        token = "test_token_12345",
        repository = "kiwiproject/kiwi-test"
    )

    private fun repoConfig() = RepoConfig(
        workingDir = File("."),
        previousRevision = "1.4.1",
        revision = "v1.4.2"
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
