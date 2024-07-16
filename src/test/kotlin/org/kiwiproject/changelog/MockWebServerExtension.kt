package org.kiwiproject.changelog

import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.kiwiproject.io.KiwiIO

/**
 * A simple JUnit Jupiter extension that creates and starts a [MockWebServer]
 * before *each* test, and shuts it down after *each* test.
 */
class MockWebServerExtension : BeforeEachCallback, AfterEachCallback {

    private var _server = MockWebServer()

    val server: MockWebServer
        get() = _server

    override fun beforeEach(context: ExtensionContext) {
        server.start()
    }

    override fun afterEach(context: ExtensionContext) {
        KiwiIO.closeQuietly(server)
    }
}

