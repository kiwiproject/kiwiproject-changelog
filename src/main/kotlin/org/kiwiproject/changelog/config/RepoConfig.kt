package org.kiwiproject.changelog.config

data class RepoConfig(
    val url: String,
    val apiUrl: String,
    val token: String,
    val repository: String,
    val previousRevision: String,
    val revision: String,
    private val milestone: String?
) {

    constructor(
        url: String,
        apiUrl: String,
        token: String,
        repository: String,
        previousRevision: String,
        revision: String
    ) : this(
        url,
        apiUrl,
        token,
        repository,
        previousRevision,
        revision,
        null
    )

    // Accepts v<major>.<minor>.<patch> and optionally a "pre-release" version
    // such as "-beta" or any other characters after the patch version.
    private val revisionPattern = Regex("v\\d+\\.\\d+\\.\\d+.*")

    fun milestone(): String {
        if (milestone != null) {
            return milestone
        }

        require(revisionPattern.matches(revision)) {
            "revision should be in the format v<major>.<minor>.<patch>"
        }

        return revision.substring(1)
    }

    fun repoUrl(): String {
        return "$url/$repository"
    }
}
