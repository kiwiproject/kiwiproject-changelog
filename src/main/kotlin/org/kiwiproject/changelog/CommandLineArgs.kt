package org.kiwiproject.changelog

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CommandLineArgs(parser: ArgParser) {

    val githubUrl by parser.storing("-g", "--github-url", help = "Url for Github").default("https://github.com")

    val githubApi by parser.storing("-a", "--api-url", help = "Url for Github API").default("https://api.github.com")

    val previousRevision by parser.storing("-p", "--previous-rev", help = "Starting revision commit to search for changes").default("master")

    val revision by parser.storing("-r", "--rev", help = "Ending revision commit to search for changes").default("HEAD")

    val date by parser.storing("-d", "--date", help = "Date/Time of the change log").default(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()))

    val outputFile by parser.storing("-o", "--output-file", help = "Location for file to output the change log").default("CONSOLE")

    val workingDir by parser.storing("-w", "--working-dir", help = "Working Directory to run Git commands") { File(this) }

    val githubToken by parser.storing("-t", "--token", help = "Authentication token for Github")

    val repository by parser.storing("-R", "--repository", help = "Name of the Github repository")

    val version by parser.storing("-v", "--version", help = "Version of the changelog being generated")
}