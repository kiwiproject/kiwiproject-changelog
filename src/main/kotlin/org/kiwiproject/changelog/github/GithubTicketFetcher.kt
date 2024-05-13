package org.kiwiproject.changelog.github

import org.kiwiproject.changelog.Ticket
import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.RepoHostConfig
import org.kiwiproject.changelog.parseTickets
import java.util.Collections
import java.util.PriorityQueue
import java.util.Queue

class GithubTicketFetcher(
    repoHostConfig: RepoHostConfig,
    private val changelogConfig: ChangelogConfig) {

    private val listFetcher = GithubListFetcher(repoHostConfig)
    private val defaultCategory = changelogConfig.categoryConfig.defaultCategory
    private val labelMapping = changelogConfig.categoryConfig.labelToCategoryMapping

    fun fetchTickets(ticketIds: List<String>) : List<Ticket> {
        if (ticketIds.isEmpty()) {
            return listOf()
        }

        println("Querying GitHub API for ${ticketIds.size} tickets.")
        val tickets = queuedTicketNumbers(ticketIds)

        val resolvedTickets : MutableList<Ticket> = mutableListOf()
        try {
            while(tickets.isNotEmpty() && listFetcher.hasNextPage()) {
                val page = listFetcher.nextPage()
                resolvedTickets.addAll(extractImprovements(dropTicketsAboveMaxInPage(tickets, page), page))
            }
        } catch (e: Exception) {
            throw RuntimeException("Problems fetching ${ticketIds.size} tickets from GitHub", e)
        }

        return resolvedTickets
    }

    private fun queuedTicketNumbers(ticketIds: List<String>) : Queue<Long> {
        val sortedTicketIds = ticketIds.map(String::toLong).sorted()
        val queue = PriorityQueue<Long>(sortedTicketIds.size, Collections.reverseOrder())
        queue.addAll(sortedTicketIds)
        return queue
    }

    private fun dropTicketsAboveMaxInPage(tickets: Queue<Long>, page: List<Map<String, Any>>) : Queue<Long> {
        if (page.isEmpty()) {
            return tickets
        }

        val highestId = page[0]["number"] as Int?
        while (tickets.isNotEmpty() && tickets.peek() > highestId!!) {
            tickets.poll()
        }

        return tickets
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractImprovements(tickets: Queue<Long>, issues: List<Map<String, Any>>) : List<Ticket> {
        if (tickets.isEmpty()) {
            return emptyList()
        }

        return issues
            .filter(this::isIssueOrLonePr)
            .map { issue ->
                val labels = issue["labels"] as List<Map<String, String>>
                val labelNames = labels.map { it["name"] as String }
                val category = findCategory(labelNames)
                Ticket(issue["number"] as Int, issue["title"] as String, issue["html_url"] as String, category)
            }
            .filter { issue -> tickets.contains(issue.id.toLong()) }
            .toList()

    }

    private fun isIssueOrLonePr(issue: Map<String, Any>) : Boolean {
        val bodyText = issue["body"] as String? ?: ""
        return issue["pull_request"] == null || parseTickets(bodyText).isEmpty() || userIsAlwaysIncluded(issue)
    }

    @Suppress("UNCHECKED_CAST")
    private fun userIsAlwaysIncluded(issue: Map<String, Any>) : Boolean {
        val user = issue["user"] as Map<String, String>
        val userName = user["login"]

        return changelogConfig.categoryConfig.alwaysIncludePRsFrom.contains(userName)
    }

    private fun findCategory(labels: List<String>) : String {
        if (labels.isEmpty()) {
            return defaultCategory
        }

        val foundLabel = labels.intersect(labelMapping.keys).firstOrNull()
        if (foundLabel == null) {
            println("WARN: Using default category ($defaultCategory) b/c no mapping found for label(s): $labels")
            return defaultCategory
        }

        return labelMapping[foundLabel] ?: error("matchingLabel should never be null here")
    }
}
