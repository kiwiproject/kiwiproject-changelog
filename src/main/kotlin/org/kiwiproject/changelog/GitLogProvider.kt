package org.kiwiproject.changelog

import org.kiwiproject.io.KiwiIO
import java.io.File

class GitLogProvider(val workingDir: File) {

    fun getLog(from: String, to: String, format: String): String {
        val fetch = "+refs/tags/$from:refs/tags/$from"
        val log = "$from..$to"

        try {
            runProcess("git", "fetch", "origin", fetch)
        } catch (e: Exception) {
            //This is a non-blocking problem because we still are able to run git log locally
            println("'git fetch' did not work, continuing running 'git log' locally.")

            //To avoid confusion, no stack trace in debug log, just the message:
            println("'git fetch' problem: ${e.message}")
        }

        return runProcess("git", "log", format, log)
    }

    private fun runProcess(vararg commandLine: String) : String {

        val output: String
        val exitValue : Int

        try {
            val process = ProcessBuilder(commandLine.asList()).directory(workingDir).redirectErrorStream(true).start()
            output = KiwiIO.readInputStreamOf(process)
            exitValue = process.waitFor();
        } catch (e: Exception) {
            throw RuntimeException("Problems executing command:\n  ${commandLine.joinToString(separator = "\n")}", e)
        }

        if (exitValue != 0) {
            val errorMsg = """
                |Problems executing command (exit code: $exitValue):
                |    command: ${commandLine.joinToString(separator = " ")}
                |    working dir: ${workingDir.absolutePath}
                |    output:
                |    $output
            """.trimMargin()

            throw RuntimeException(errorMsg)
        }

        return output
    }
}
