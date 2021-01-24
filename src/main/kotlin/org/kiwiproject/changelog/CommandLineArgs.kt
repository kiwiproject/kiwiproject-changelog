package org.kiwiproject.changelog

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import org.kiwiproject.changelog.config.OutputType
import java.io.File

class CommandLineArgs(parser: ArgParser) {

    // Github Options

    val githubApi by parser.storing("-a", "--github-api-url", help = "Url for Github API").default("https://api.github.com")
    val repository by parser.storing("-r", "--github-repository", help = "Name of the Github repository")
    val githubToken by parser.storing("-t", "--github-token", help = "Authentication token for Github")
    val githubUrl by parser.storing("-u", "--github-url", help = "Url for Github").default("https://github.com")

    // Git Repo Options

    val previousRevision by parser.storing("-p", "--previous-rev", help = "Starting revision commit to search for changes").default("master")
    val revision by parser.storing("-R", "--rev", help = "Ending revision commit to search for changes").default("HEAD")
    val workingDir by parser.storing("-w", "--working-dir", help = "Working Directory to run Git commands") { File(this) }

    // Change Log Options

    val outputType by parser.mapping(
        "--console" to OutputType.CONSOLE,
        "--file" to OutputType.FILE,
        "--github" to OutputType.GITHUB_RELEASE,
        help = "How the changelog should be output"
    ).default(OutputType.CONSOLE)
    val outputFile by parser.storing("-o", "--output-file", help = "Location for file to output the change log").default<String?>(null)
    val version by parser.storing("-v", "--version", help = "Version of the changelog being generated")

    // Category Options

    val defaultCategory by parser.storing("-c", "--default-category", help = "Default category to put issues into if a mapping is not provided").default("Assorted")
    val alwaysIncludePRsFrom by parser.adding("-i", "--include-prs-from", help = "Always include PRs in the output from this user (e.g. dependabot)")
    val labelToCategoryMapping by parser.adding("-m", "--mapping", help = "Map a label to a category (format = label:category")
    val categoryOrder by parser.adding("-O", "--category-order", help = "The order to display the categories. Order is the order of these options")
}