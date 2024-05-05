package org.kiwiproject.changelog

import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.RepoConfig
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
        "improvements" to formatImprovements(tickets, changelogConfig.categoryConfig.categoryOrder)
    )

    return replaceTokens(template, logData)
}

fun formatImprovements(tickets: List<Ticket>, categoryOrder: List<String>?) : String {
    if (tickets.isEmpty()) {
        return " - No notable improvements. No pull requests (issues) were referenced from commits."
    }

    val groupedTickets = tickets.groupBy { it.category }.toSortedMap()

    val categories = if (categoryOrder.isNullOrEmpty()) groupedTickets.keys else categoryOrder

    var improvementText = ""

    for (category in categories) {
        if (!groupedTickets.containsKey(category)) {
            continue
        }

        improvementText += "## $category\n"

        for (ticket in groupedTickets[category] ?: listOf()) {
            improvementText += "* ${ticket.title} [(#${ticket.id})](${ticket.url})\n"
        }
    }

    return improvementText
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
