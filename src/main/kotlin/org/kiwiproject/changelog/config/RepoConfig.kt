package org.kiwiproject.changelog.config

import java.io.File

data class RepoConfig(val workingDir: File = File("."),
                      val previousRevision: String = "master",
                      val revision: String = "HEAD")
