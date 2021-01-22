package org.kiwiproject.changelog

class GitCommit(val author: String, message: String) {

    val tickets: Set<String> = parseTickets(message)

}
