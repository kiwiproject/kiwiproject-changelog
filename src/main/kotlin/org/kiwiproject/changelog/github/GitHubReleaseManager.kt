package org.kiwiproject.changelog.github

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.kiwiproject.changelog.config.RepoConfig

/**
 * Provides a simple way to interact with GitHub releases.
 */
class GitHubReleaseManager(
    private val repoConfig: RepoConfig,
    private val api: GitHubApi,
    private val mapper: ObjectMapper
) {

    private val createReleaseUrl = "${repoConfig.apiUrl}/repos/${repoConfig.repository}/releases"

    /**
     * Create a new release in GitHub for the given [tagName]
     * with [releaseContent] as the contents of the new release.
     *
     * The tag must exist, otherwise an [IllegalStateException] is thrown.
     *
     * The release must not exist. If it does, an [IllegalStateException] is thrown.
     *
     * An [IllegalStateException] is thrown if the HTTP response from GitHub
     * is not 201 Created.
     */
    fun createRelease(tagName: String, releaseContent: String): GitHubRelease {
        checkTagExists(tagName)
        checkReleaseDoesNotExist(tagName)

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
        "${repoConfig.apiUrl}/repos/${repoConfig.repository}/git/ref/tags/$tagName"

    private fun checkReleaseDoesNotExist(tagName: String) {
        // Implementation Note:
        // This gets the 100 most recent releases. We do not bother paginating,
        // and assume that no one would realistically try to release something
        // that old. So, if they do, we will rely on GitHub returning a 422 if
        // the release exists.

        val releasesUrl = listReleasesUrlFor()
        val listReleasesResponse = api.get(releasesUrl)
        check(listReleasesResponse.statusCode == 200) {
            "List releases was unsuccessful. Status: ${listReleasesResponse.statusCode}. Text: ${listReleasesResponse.content}"
        }

        val releasesList = mapper.readValue<List<Map<String, Any>>>(listReleasesResponse.content)
        val releases = releasesList.map { release ->
            mapOf("name" to release["name"], "tag_name" to release["tag_name"])
        }

        checkReleaseDoesNotExist(releases, tagName, "name", "with name")
        checkReleaseDoesNotExist(releases, tagName, "tag_name", "associated with tag")
    }

    private fun listReleasesUrlFor() =
        "${repoConfig.apiUrl}/repos/${repoConfig.repository}/releases?per_page=100"

    private fun checkReleaseDoesNotExist(
        releases: List<Map<String, Any?>>,
        tagName: String,
        property: String,
        failurePhrase: String
    ) {
        val releaseDoesNotExist = releases.none { release -> release[property] == tagName }
        check(releaseDoesNotExist) { "A release $failurePhrase $tagName already exists" }
    }
}

data class GitHubRelease(val htmlUrl: String) {
    companion object {
        fun from(responseContent: Map<String, Any>): GitHubRelease {
            val htmlUrl = responseContent["html_url"] as String
            return GitHubRelease(htmlUrl)
        }
    }
}
