package org.kiwiproject.changelog

import java.util.Collections
import java.util.PriorityQueue
import java.util.Queue

class GithubTicketFetcher(apiUrl: String, repository: String, githubToken: String) {

    private val listFetcher = GithubListFetcher(apiUrl, repository, githubToken)

    fun fetchTickets(ticketIds: List<String>) : List<Ticket> {
        if (ticketIds.isEmpty()) {
            return listOf()
        }

        println("Querying Github API for ${ticketIds.size} tickets.")
        val tickets = queuedTicketNumbers(ticketIds)

        val resolvedTickets : MutableList<Ticket> = mutableListOf()
        try {
            while(tickets.isNotEmpty() && listFetcher.hasNextPage()) {
                val page = listFetcher.nextPage()
                resolvedTickets.addAll(extractImprovements(dropTicketsAboveMaxInPage(tickets, page), page))
            }
        } catch (e: Exception) {
            throw RuntimeException("Problems fetching ${ticketIds.size} tickets from Github", e)
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
        while (!tickets.isEmpty() && tickets.peek() > highestId!!) {
            tickets.poll()
        }

        return tickets
    }

    private fun extractImprovements(tickets: Queue<Long>, issues: List<Map<String, Any>>) : List<Ticket> {
        if (tickets.isEmpty()) {
            return emptyList()
        }

//        val pagedTickets : MutableList<Ticket> = mutableListOf()

        return issues
            .map { issue -> Ticket(issue["number"] as Int?, issue["title"] as String?, issue["html_url"] as String?) }
            .filter { issue -> tickets.contains(issue.id?.toLong()) }
            .toList()

//        for(issue in issues) {
//            val ticket = Ticket(issue["number"] as Int?, issue["title"] as String?, issue["html_url"] as String?)
//            if (tickets.remove(ticket.id)) {
//                pagedTickets.add(ticket)
//                if (tickets.isEmpty()) {
//                    return pagedTickets
//                }
//            }
//        }
//
//        return pagedTickets
    }
}
