package org.kiwiproject.changelog.config

data class RepoHostConfig(val url: String,
                          val apiUrl: String,
                          val token: String,
                          val repository: String) {

    fun fullRepoUrl() : String {
        return "$apiUrl/$repository"
    }
}
