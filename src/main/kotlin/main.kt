import com.xenomachina.argparser.ArgParser
import org.kiwiproject.changelog.CommandLineArgs
import org.kiwiproject.changelog.GenerateChangelog
import org.kiwiproject.changelog.config.CategoryConfig
import org.kiwiproject.changelog.config.ChangelogConfig
import org.kiwiproject.changelog.config.GitRepoConfig
import org.kiwiproject.changelog.config.GithubConfig
import java.io.File

fun main(args: Array<String>) {
    ArgParser(args).parseInto(::CommandLineArgs).run {
        val githubConfig = GithubConfig(githubUrl, githubApi, githubToken, repository)
        val gitRepoConfig = GitRepoConfig(workingDir, previousRevision, revision)

        val categoryConfig = CategoryConfig(defaultCategory, convertMappings(labelToCategoryMapping), alwaysIncludePRsFrom, categoryOrder)

        val out = if (outputFile == null) {
            null
        } else {
            File(outputFile!!)
        }

        val changeLogConfig = ChangelogConfig(version = version, outputType = outputType, outputFile = out, categoryConfig = categoryConfig)

        GenerateChangelog(githubConfig, gitRepoConfig, changeLogConfig).generate()
    }
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