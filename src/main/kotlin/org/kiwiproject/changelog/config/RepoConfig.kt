package org.kiwiproject.changelog.config

import java.io.File

// TODO Remove the default values; these should be required here without default values
data class RepoConfig(val workingDir: File = File("."),
                      val previousRevision: String = "main",
                      val revision: String = "HEAD")
