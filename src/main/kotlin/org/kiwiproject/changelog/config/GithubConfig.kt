package org.kiwiproject.changelog.config

data class GithubConfig(val url: String = "https://github.com",
                        val apiUrl: String = "https://api.github.com",
                        val token: String,
                        val repository: String) {

    fun fullRepoUrl() : String {
        return "$apiUrl/$repository"
    }
}
