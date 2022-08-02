package org.kiwiproject.changelog

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.multiple
import kotlinx.cli.required
import org.kiwiproject.changelog.config.OutputType
import java.nio.file.Paths

class CommandLineArgs(parser: ArgParser) {

    enum class Provider {
        GITHUB,
        GITLAB
    }

    // Github/Gitlab Options
    val provider by parser.option(ArgType.Choice<Provider>(), shortName = "P", description = "Host Git repository provider").default(Provider.GITHUB)

    val repoHostApi by parser.option(ArgType.String, shortName = "a", fullName = "repo-host-api-url", description = "Url for Github or Gitlab API")
    val repository by parser.option(ArgType.String, shortName = "r", description = "Name of the Github or Gitlab repository").required()
    val repoHostToken by parser.option(ArgType.String, shortName = "t", fullName = "repo-host-token", description = "Authentication token for Github or Gitlab").required()
    val repoHostUrl by parser.option(ArgType.String, shortName = "u", fullName = "repo-host-url", description = "Url for Github or Gitlab")

    // Git Repo Options
    val previousRevision by parser.option(ArgType.String, shortName = "p", fullName = "previous-rev", description = "Starting revision commit to search for changes").default("master")
    val revision by parser.option(ArgType.String, shortName = "R", description = "Ending revision commit to search for changes").default("HEAD")
    val workingDir by parser.option(ArgType.String, shortName = "w", fullName = "working-dir", description = "Working Directory to run Git commands").default(Paths.get("").toAbsolutePath().toString())

    // Change Log Options
    val outputType by parser.option(ArgType.Choice<OutputType>(), shortName = "ot", fullName = "output-type", description = "How the changelog should be output").default(OutputType.CONSOLE)
    val outputFile by parser.option(ArgType.String, shortName = "o", fullName = "output-file", description = "Location for file to output the change log. Only needed if output type is file.")

    // Category Options
    val defaultCategory by parser.option(ArgType.String, shortName = "c", fullName = "default-category", description = "Default category to put issues into if a mapping is not provided").default("Assorted")
    val alwaysIncludePRsFrom by parser.option(ArgType.String, shortName = "i", fullName = "include-prs-from", description = "Always include PRs in the output from this user (e.g. dependabot)").multiple()
    val labelToCategoryMapping by parser.option(ArgType.String, shortName = "m", fullName = "mapping", description = "Map a label to a category (format = label:category)").multiple()
    val categoryOrder by parser.option(ArgType.String, shortName = "O", fullName = "category-order", description = "The order to display the categories. Order is the order of these options").multiple()
}
