import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import org.kiwiproject.changelog.CommandLineArgs
import org.kiwiproject.changelog.GenerateChangelog
import org.kiwiproject.changelog.config.CategoryConfig
import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.RepoConfig
import org.kiwiproject.changelog.config.RepoHostConfig
import java.io.File

fun main(args: Array<String>) = mainBody {
    ArgParser(args).parseInto(::CommandLineArgs).run {
        val repoHostConfig = buildRepoHostConfig(this)
        println(repoHostConfig)
        val repoConfig = RepoConfig(workingDir, previousRevision, revision)

        val categoryConfig = CategoryConfig(defaultCategory, convertMappings(labelToCategoryMapping), alwaysIncludePRsFrom, categoryOrder)

        val out = if (outputFile == null) {
            null
        } else {
            File(outputFile!!)
        }

        val changeLogConfig = ChangelogConfig(version = version, outputType = outputType, outputFile = out, categoryConfig = categoryConfig)

        GenerateChangelog(repoHostConfig, repoConfig, changeLogConfig).generate()
    }
}

private fun buildRepoHostConfig(args: CommandLineArgs) : RepoHostConfig {
    val repoHostUrl = resolveHostUrl(args)
    val repoHostApi = resolveHostAPI(args)

    return RepoHostConfig(repoHostUrl, repoHostApi, repoHostToken, repository)
}

private fun resolveHostUrl(args: CommandLineArgs) : String {
    if (args.repoHostUrl.isNotBlank()) {
        return args.repoHostUrl
    }

    if (args.useGithub as Boolean) {
        return "https://github.com"
    }

    if (args.useGithub as Boolean) {
        return "https://gitlab.com"
    }

    throw IllegalArgumentException("--repo-host-url, --use-github, or --use-gitlab must be provided")
}

private fun resolveHostAPI(args: CommandLineArgs) : String {
    if (args.repoHostApi.isNotBlank()) {
        return args.repoHostApi
    }

    if (args.useGithub as Boolean) {
        return "https://api.github.com"
    }

    if (args.useGithub as Boolean) {
        return "https://gitlab.com/api/v4"
    }

    throw IllegalArgumentException("--repo-host--api-url, --use-github, or --use-gitlab must be provided")
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
