package org.kiwiproject.changelog

import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.extension.doesNotContainKey
import java.util.regex.Pattern

fun formatChangeLog(contributors: Set<String>,
                    tickets: List<Ticket>,
                    commitCount: Int,
                    repoConfig: RepoConfig,
                    changelogConfig: ChangelogConfig,
                    githubRepoUrl: String) : String {

    val template = """
        ## Summary
        - @date@ - [@commitCount@ commit(s)](@repoUrl@/compare/@previousRev@...@newRev@) by @contributors@
        @improvements@
    """.trimIndent()

    val logData = mapOf(
        "date" to changelogConfig.dateString,
        "commitCount" to "$commitCount",
        "repoUrl" to githubRepoUrl,
        "previousRev" to repoConfig.previousRevision,
        "newRev" to repoConfig.revision,
        "contributors" to contributors.joinToString(", "),
        "improvements" to formatImprovements(
            tickets,
            changelogConfig.categoryConfig.categoryOrder,
            changelogConfig.categoryConfig.categoryToEmoji
        )
    )

    return replaceTokens(template, logData)
}

fun formatImprovements(
    tickets: List<Ticket>,
    categoryOrder: List<String>,
    categoryToEmoji: Map<String, String?>
): String {

    if (tickets.isEmpty()) {
        return " - No notable improvements. No pull requests (issues) were referenced from commits."
    }

    val groupedTickets = tickets.groupBy { it.category }.toSortedMap()
    val ticketCategories = groupedTickets.keys

    // Ensure all categories exist, and add them if not
    // (otherwise, those tickets won't be in the change log)
    val categories = ensureAllCategories(categoryOrder, ticketCategories)

    var improvementText = ""

    for (category in categories) {
        if (groupedTickets.doesNotContainKey(category)) {
            continue
        }

        val emoji = categoryToEmoji[category]
        val categoryText = if (emoji == null) category else "$category $emoji"

        improvementText += "## $categoryText\n"

        for (ticket in groupedTickets[category] ?: listOf()) {
            improvementText += "* ${ticket.title} [(#${ticket.id})](${ticket.url})\n"
        }
    }

    return improvementText
}

fun ensureAllCategories(
    categoryOrder: List<String>,
    ticketCategories: Set<String>
): Collection<String> {
    require(ticketCategories.isNotEmpty()) { "ticketCategories must not be empty" }

    val initialCategories = categoryOrder.ifEmpty { ticketCategories }
    val missingCategories = ticketCategories - initialCategories.toSet();
    if (missingCategories.isNotEmpty()) {
        println("WARN: Missing categories ${missingCategories.joinToString()} (they will be added after other categories)")
    }
    return when {
        missingCategories.isNotEmpty() -> initialCategories + missingCategories.sorted()
        else -> initialCategories
    }
}

fun replaceTokens(template: String, data: Map<String, String>) : String {
    val pattern = Pattern.compile("@(.+?)@")
    val matcher = pattern.matcher(template)
    val buffer = StringBuffer()

    while (matcher.find()) {
        val replacement: String? = data[matcher.group(1)]
        if (replacement != null) {
            matcher.appendReplacement(buffer, "")
            buffer.append(replacement)
        }
    }
    matcher.appendTail(buffer)
    return buffer.toString()
}
