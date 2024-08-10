package org.kiwiproject.changelog.github

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.kiwiproject.changelog.extension.nowUtc

@DisplayName("GitHubChange")
class GitHubChangeTest {

    @Test
    fun shouldCreateFromFactory() {
        val issue = GitHubIssue(
            "Improve the foo",
            42,
            "https://fake-github.com/fakeorg/fakerepo/issues/43",
            listOf("enhancement"),
            null,
            nowUtc()
        )

        val change = GitHubChange.from(issue, "Improvements")

        assertAll(
            { assertThat(change.title).isEqualTo(issue.title) },
            { assertThat(change.number).isEqualTo(issue.number) },
            { assertThat(change.htmlUrl).isEqualTo(issue.htmlUrl) },
            { assertThat(change.htmlUrl).isEqualTo(issue.htmlUrl) },
            { assertThat(change.category).isEqualTo("Improvements") }
        )
    }

    @Test
    fun shouldRenderAsMarkdown() {
        val change = GitHubChange(
            420,
            "Make the baz better",
            "https://fake-github.com/fakeorg/fakerepo/issues/420",
            "Refactoring"
        )

        assertThat(change.asMarkdown())
            .isEqualTo("Make the baz better [(#420)](https://fake-github.com/fakeorg/fakerepo/issues/420)")
    }
}
