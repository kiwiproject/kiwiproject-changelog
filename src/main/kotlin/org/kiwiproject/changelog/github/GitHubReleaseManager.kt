package org.kiwiproject.changelog.github

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.kiwiproject.changelog.config.RepoHostConfig

/**
 * Provides a simple way to interact with GitHub releases.
 */
class GitHubReleaseManager(
    private val repoHostConfig: RepoHostConfig,
    private val api: GithubApi,
    private val mapper: ObjectMapper
) {

    private val createReleaseUrl = "${repoHostConfig.apiUrl}/repos/${repoHostConfig.repository}/releases"

    /**
     * Create a new release in GitHub for the given [tagName]
     * with [releaseContent] as the contents of the new release.
     *
     * The tag must exist, otherwise an [IllegalStateException] is thrown.
     *
     * An [IllegalStateException] is thrown if the HTTP response from GitHub
     * is not 201 Created.
     */
    fun createRelease(tagName: String, releaseContent: String): GitHubRelease {
        checkTagExists(tagName)

        val bodyParameters = mapOf<String, Any>(
            "tag_name" to tagName,
            "name" to tagName,
            "body" to releaseContent
        )
        val bodyJson = mapper.writeValueAsString(bodyParameters)
        val response = api.post(createReleaseUrl, bodyJson)
        check(response.statusCode == 201) {
            "Create release was unsuccessful. Status: ${response.statusCode}. Text: ${response.content}"
        }

        val responseContent = mapper.readValue<Map<String, Any>>(response.content)
        return GitHubRelease.from(responseContent)
    }

    private fun checkTagExists(tagName: String) {
        val tagUrl = tagUrlFor(tagName)
        val getTagResponse = api.get(tagUrl)
        check(getTagResponse.statusCode == 200) {
            "Get tag was unsuccessful. Status: ${getTagResponse.statusCode}. Text: ${getTagResponse.content}"
        }
    }

    private fun tagUrlFor(tagName: String) =
        "${repoHostConfig.apiUrl}/repos/${repoHostConfig.repository}/git/ref/tags/$tagName"

    data class GitHubRelease(val htmlUrl: String) {
        companion object {
            fun from(responseContent: Map<String, Any>): GitHubRelease {
                val htmlUrl = responseContent["html_url"] as String
                return GitHubRelease(htmlUrl)
            }
        }
    }
}
