package org.kiwiproject.changelog.junit

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintStream

class StdIoExtension : BeforeEachCallback, AfterEachCallback {

    private lateinit var originalSystemOut: PrintStream
    private lateinit var outputStream: ByteArrayOutputStream

    private lateinit var originalSystemIn: InputStream
    private lateinit var inputStream: ByteArrayInputStream

    override fun beforeEach(context: ExtensionContext) {
        // store and replace the original System.out
        originalSystemOut = System.out

        outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        // store the original System.in
        originalSystemIn = System.`in`
    }

    override fun afterEach(context: ExtensionContext) {
        // restore
        System.setOut(originalSystemOut)
        System.setIn(originalSystemIn)
    }

    @Suppress("unused")
    fun setInput(input: String) {
        inputStream = ByteArrayInputStream(input.toByteArray())
        System.setIn(inputStream)
    }

    fun capturedLines() : List<String> {
        return outputStream.toString().split(SEPARATOR.toRegex())
    }

    companion object {
        val SEPARATOR: String = System.lineSeparator()
    }
}
