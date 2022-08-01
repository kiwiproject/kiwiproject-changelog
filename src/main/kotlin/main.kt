import org.kiwiproject.base.KiwiPreconditions.checkArgumentNotBlank
import org.kiwiproject.changelog.CommandLineArgs
import org.kiwiproject.changelog.GenerateChangelog
import org.kiwiproject.changelog.config.CategoryConfig
import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.config.RepoHostConfig
import java.io.File
import kotlinx.cli.ArgParser

fun main(args: Array<String>) {
    val parser = ArgParser("changelog")
    val cliArgs = CommandLineArgs(parser)
    
    parser.parse(args)

    val repoHostConfig = buildRepoHostConfig(cliArgs)
    val repoConfig = RepoConfig(cliArgs.workingDir, cliArgs.previousRevision, cliArgs.revision)

    val categoryConfig = CategoryConfig(cliArgs.defaultCategory, convertMappings(cliArgs.mapping), cliArgs.includePrsFrom, cliArgs.categoryOrder)

    val out = if (cliArgs.outputFile == null) {
        null
    } else {
        File(cliArgs.outputFile!!)
    }

    val changeLogConfig = ChangelogConfig(version = cliArgs.version, outputType = cliArgs.outputType, outputFile = out, categoryConfig = categoryConfig)

    GenerateChangelog(repoHostConfig, repoConfig, changeLogConfig).generate()
}

private fun buildRepoHostConfig(args: CommandLineArgs) : RepoHostConfig {
    val repoHostUrl = resolveHostUrl(args)
    val repoHostApi = resolveHostAPI(args)
    val repoHostToken = resolveRepoHostToken(args)
    val repository = resolveRepository(args)

    return RepoHostConfig(repoHostUrl, repoHostApi, repoHostToken, repository)
}

private fun resolveHostUrl(args: CommandLineArgs) : String {
    if (args.repoHostUrl?.isEmpty() ?: true) {
        return args.repoHostUrl!!
    }

    if (args.useGithub) {
        return "https://github.com"
    }

    if (args.useGitlab) {
        return "https://gitlab.com"
    }

    throw IllegalArgumentException("--repo-host-url, --use-github, or --use-gitlab must be provided")
}

private fun resolveHostAPI(args: CommandLineArgs) : String {
    if (args.repoHostApi?.isEmpty() ?: true) {
        return args.repoHostApi!!
    }

    if (args.useGithub) {
        return "https://api.github.com"
    }

    if (args.useGitlab) {
        return "https://gitlab.com/api/v4"
    }

    throw IllegalArgumentException("--repo-host-api-url, --use-github, or --use-gitlab must be provided")
}

private fun resolveRepoHostToken(args: CommandLineArgs) : String {
    checkArgumentNotBlank(args.repoHostToken, "--repo-host-token is required")

    return args.repoHostToken
}

private fun resolveRepository(args: CommandLineArgs) : String {
    checkArgumentNotBlank(args.repository, "--repository is required")

    return args.repository
}

private fun convertMappings(labelToCategoryMapping: List<String>?) : Map<String, String> {
    if (labelToCategoryMapping == null) {
        return emptyMap()
    }

    return labelToCategoryMapping
        .asSequence()
        .map { opt -> opt.split(":") }
        .map { parts -> parts[0] to parts[1] }
        .toMap()
}
