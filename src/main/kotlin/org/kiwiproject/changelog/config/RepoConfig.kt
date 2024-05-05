package org.kiwiproject.changelog.config

import java.io.File

data class RepoConfig(val workingDir: File,
                      val previousRevision: String,
                      val revision: String)
