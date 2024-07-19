package org.kiwiproject.changelog

import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.extension.doesNotContainKey
import org.kiwiproject.changelog.github.GitHubChange
import org.kiwiproject.changelog.github.GitHubSearchManager.CommitAuthorsResult
import java.util.regex.Pattern

fun formatChangeLog(
    commitAuthorsResult: CommitAuthorsResult,
    gitHubChanges: List<GitHubChange>,
    repoConfig: RepoConfig,
    changelogConfig: ChangelogConfig,
    githubRepoUrl: String
) : String {

    val template = """
        ## Summary
        - @date@ - [@commitCount@ commit(s)](@repoUrl@/compare/@previousRev@...@newRev@) by @authors@

        @changes@
    """.trimIndent()

    val authors = commitAuthorsResult.authors.joinToString(", ") { author -> author.asMarkdown() }

    val logData = mapOf(
        "date" to changelogConfig.dateString,
        "commitCount" to "${commitAuthorsResult.totalCommits}",
        "repoUrl" to githubRepoUrl,
        "previousRev" to repoConfig.previousRevision,
        "newRev" to repoConfig.revision,
        "authors" to authors,
        "changes" to formatChanges(
            gitHubChanges,
            repoConfig.milestone(),
            changelogConfig.categoryConfig.categoryOrder,
            changelogConfig.categoryConfig.categoryToEmoji
        )
    )

    return replaceTokens(template, logData)
}

fun formatChanges(
    gitHubChanges: List<GitHubChange>,
    milestone: String,
    categoryOrder: List<String>,
    categoryToEmoji: Map<String, String?>
): String {

    if (gitHubChanges.isEmpty()) {
        return " - No notable improvements. No issues or pull requests referenced milestone $milestone."
    }

    val groupedChanges = gitHubChanges.groupBy { it.category }.toSortedMap()
    val changeCategories = groupedChanges.keys

    // Ensure all categories exist, and add them if not
    // (otherwise, those changes won't be in the change log)
    val categories = ensureAllCategories(categoryOrder, changeCategories)

    var markdown = ""

    for (category in categories) {
        if (groupedChanges.doesNotContainKey(category)) {
            continue
        }

        val emoji = categoryToEmoji[category]
        val categoryText = if (emoji == null) category else "$category $emoji"

        markdown += "## $categoryText\n"

        for (change in groupedChanges[category] ?: listOf()) {
            markdown += "* ${change.asMarkdown()}\n"
        }
        markdown += "\n"
    }

    return markdown
}

internal fun ensureAllCategories(
    categoryOrder: List<String>,
    categories: Set<String>
): Collection<String> {
    require(categories.isNotEmpty()) { "categories must not be empty" }

    val initialCategories = categoryOrder.ifEmpty { categories }
    val missingCategories = categories - initialCategories.toSet()
    if (missingCategories.isNotEmpty()) {
        println("⚠ WARN: Missing categories ${missingCategories.joinToString()} (they will be added after other categories)")
    }
    return when {
        missingCategories.isNotEmpty() -> initialCategories + missingCategories.sorted()
        else -> initialCategories
    }
}

private fun replaceTokens(template: String, data: Map<String, String>) : String {
    val pattern = Pattern.compile("@(.+?)@")
    val matcher = pattern.matcher(template)
    val builder = StringBuilder()

    while (matcher.find()) {
        val replacement: String? = data[matcher.group(1)]
        if (replacement != null) {
            matcher.appendReplacement(builder, "")
            builder.append(replacement)
        }
    }
    matcher.appendTail(builder)
    return builder.toString()
}
