package org.kiwiproject.changelog

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import org.kiwiproject.changelog.config.OutputType
import java.nio.file.Paths

class CommandLineArgs(parser: ArgParser) {

    // Github/Gitlab Options
    val useGithub by parser.option(ArgType.Boolean, shortName = "gh", description = "Use Github default settings").default(true)
    val useGitlab by parser.option(ArgType.Boolean, shortName = "gl", description = "Use Gitlab default settings").default(false)

    val repoHostApi by parser.option(ArgType.String, shortName = "-a", description = "Url for Github or Gitlab API")
    val repository by parser.option(ArgType.String, shortName = "-r", description = "Name of the Github or Gitlab repository").required()
    val repoHostToken by parser.option(ArgType.String, shortName = "-t", description = "Authentication token for Github or Gitlab").required()
    val repoHostUrl by parser.option(ArgType.String, shortName = "-u", description = "Url for Github or Gitlab")

    // Git Repo Options
    val previousRevision by parser.option(ArgType.String, shortName = "-p", description = "Starting revision commit to search for changes").default("master")
    val revision by parser.option(ArgType.String, shortName = "-R", description = "Ending revision commit to search for changes").default("HEAD")
    val workingDir by parser.option(ArgType.String, shortName = "-w", description = "Working Directory to run Git commands").default(Paths.get("").toAbsolutePath().toString())

    // Change Log Options
    val outputType by parser.option(ArgType.Choice<OutputType>(), shortName = "ot", description = "How the changelog should be output").default(OutputType.CONSOLE)
    val outputFile by parser.option(ArgType.String, shortName = "-o", description = "Location for file to output the change log")
    val version by parser.option(ArgType.String, shortName = "-v", description = "Version of the changelog being generated")

    // Category Options
    val defaultCategory by parser.option(ArgType.String, shortName = "-c", description = "Default category to put issues into if a mapping is not provided").default("Assorted")
    val includePrsFrom by parser.option(ArgType.String, shortName = "-i", description = "Always include PRs in the output from this user (e.g. dependabot)").multiple()
    val mapping by parser.option(ArgType.String, shortName = "-m", description = "Map a label to a category (format = label:category").multiple()
    val categoryOrder by parser.option(ArgType.String, shortName = "-O", description = "The order to display the categories. Order is the order of these options").multiple()
}