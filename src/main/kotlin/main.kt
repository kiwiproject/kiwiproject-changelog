import com.xenomachina.argparser.ArgParser
import org.kiwiproject.changelog.CommandLineArgs
import org.kiwiproject.changelog.GenerateChangelog

fun main(args: Array<String>) {
    ArgParser(args).parseInto(::CommandLineArgs).run {
        GenerateChangelog(githubUrl, workingDir, githubToken, githubApi, repository, previousRevision, version, revision, date, outputFile).generate()
    }
}