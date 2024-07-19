package org.kiwiproject.changelog.config

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("RepoConfig")
class RepoConfigTest {

    private val url = "https://fake-github.com"
    private val apiUrl = "https://api.fake-github.com"
    private val token = "12345"
    private val repository = "fakeorg/fakerepo"
    private val previousRevision = "v1.4.1"

    @Nested
    inner class Milestone {

        @Test
        fun shouldReturnNonNullValue_IfProvidedInConstructor() {
            val repoConfig = RepoConfig(
                url,
                apiUrl,
                token,
                repository,
                "rel-1.4.1",
                "rel-1.4.2",
                "1.4.2"
            )

            assertThat(repoConfig.milestone()).isEqualTo("1.4.2")
        }

        @ParameterizedTest
        @CsvSource(textBlock = """
            v1.4.2, 1.4.2
            v1.5.0, 1.5.0
            v2.0.0, 2.0.0
            v2.0.0-alpha, 2.0.0-alpha
            v2.0.0-beta, 2.0.0-beta
            v2.0.0-gamma, 2.0.0-gamma""")
        fun shouldRemoveLeading_v_FromRevision(revision: String, expectedRevision: String) {
            val repoConfig = RepoConfig(url, apiUrl, token, repository, previousRevision, revision, milestone = null)

            assertThat(repoConfig.milestone()).isEqualTo(expectedRevision)
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "",
            "1.4.2",
            "v1",
            "v1.4",
            "v1.4.",
        ])
        fun shouldThrowIllegalArgument_WhenRevisionIsNotInExpectedFormat(revision: String) {
            val repoConfig = RepoConfig(url, apiUrl, token, repository, previousRevision, revision, milestone = null)

            assertThatIllegalArgumentException().isThrownBy { repoConfig.milestone() }
        }
    }

    @Test
    fun shouldCreateRepoUrl() {
        val repoConfig = RepoConfig(url, apiUrl, token, repository, previousRevision, "1.4.2", milestone = null)

        assertThat(repoConfig.repoUrl()).isEqualTo("$url/$repository")
    }
}
