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
    val repoHostUrl = if (args.githubUrl.isNotBlank()) {
        println("WARNING: --github-url is deprecated and will be removed one day. Please use --repo-host-url")
        args.githubUrl
    } else {
        args.repoHostUrl
    }

    val repoHostApi = if (args.githubApi.isNotBlank()) {
        println("WARNING: --github-api-url is deprecated and will be removed one day. Please use --repo-host-api-url")
        args.githubApi
    } else {
        args.repoHostApi
    }

    val repoHostToken = if (args.githubToken.isNotBlank()) {
        println("WARNING: --github-token is deprecated and will be removed one day. Please use --repo-host-token")
        args.githubToken
    } else {
        args.repoHostToken
    }

    val repository = if (args.githubRepository.isNotBlank()) {
        println("WARNING: --github-repository is deprecated and will be removed one day. Please use --repository")
        args.githubRepository
    } else {
        args.repository
    }

    return RepoHostConfig(repoHostUrl, repoHostApi, repoHostToken, repository)
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
