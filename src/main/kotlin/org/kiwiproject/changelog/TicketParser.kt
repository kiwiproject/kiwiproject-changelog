package org.kiwiproject.changelog

fun parseTickets(text: String) : Set<String> {
    val pattern = "#\\d+".toRegex()
    return pattern
        .findAll(text)
        .map(MatchResult::value)
        .map{ num -> num.substring(1)} // remove the #
        .toSet()
}