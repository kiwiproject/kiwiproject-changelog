package org.kiwiproject.changelog.github

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.kiwiproject.changelog.config.RepoConfig

/**
 * Provides a simple way to interact with GitHub milestones.
 */
class GitHubMilestoneManager(
    private val repoConfig: RepoConfig,
    private val api: GitHubApi,
    private val mapper: ObjectMapper
) {

    private val listOpenMilestonesUrl =
        "${repoConfig.apiUrl}/repos/${repoConfig.repository}/milestones?state=open&per_page=50"

    fun getOpenMilestoneByTitle(title: String): GitHubMilestone {
        val maybeMilestone = getOpenMilestoneByTitleOrNull(title)
        check(maybeMilestone != null) {
            "No milestone with title $title was found"
        }

        return maybeMilestone
    }

    fun getOpenMilestoneByTitleOrNull(title: String): GitHubMilestone? {
        val response = api.get(listOpenMilestonesUrl)
        check(response.statusCode == 200) {
            "List milestones request failed. Status: ${response.statusCode}. Text: ${response.content}"
        }

        // Uses "per_page" of 50, assuming there will never be anywhere close to that number.
        // If there are, we'll see a link header, and will consider that an illegal state.
        check(response.linkHeader == null) {
            "We received a Link header when we were not expecting it!"
        }

        val milestones = mapper.readValue<List<Map<String, Any>>>(response.content)
        val foundMilestone: Map<String, Any>? = milestones.firstOrNull { it["title"] as String == title }

        return when (foundMilestone) {
            null -> null
            else -> GitHubMilestone.from(foundMilestone)
        }
    }

    fun closeMilestone(number: Int): GitHubMilestone {
        val closeUrl = closeMilestoneUrl(number)
        val bodyParameters = mapOf<String, Any>(
            "state" to "closed"
        )
        val bodyJson = mapper.writeValueAsString(bodyParameters)

        val response = api.patch(closeUrl, bodyJson)
        check(response.statusCode == 200) {
            "Close milestone $number was unsuccessful. Status: ${response.statusCode}. Text: ${response.content}"
        }

        val responseContent = mapper.readValue<Map<String, Any>>(response.content)
        return GitHubMilestone.from(responseContent)
    }

    private fun closeMilestoneUrl(number: Int): String =
        "${repoConfig.apiUrl}/repos/${repoConfig.repository}/milestones/$number"

    fun createMilestone(title: String): GitHubMilestone {
        val createUrl = createMilestoneUrl()
        val bodyParameters = mapOf<String, Any>(
            "title" to title
        )
        val bodyJson = mapper.writeValueAsString(bodyParameters)

        val response = api.post(createUrl, bodyJson)
        check(response.statusCode == 201) {
            "Create milestone $title was unsuccessful. Status: ${response.statusCode}. Text: ${response.content}"
        }

        val responseContent = mapper.readValue<Map<String, Any>>(response.content)
        return GitHubMilestone.from(responseContent)
    }

    private fun createMilestoneUrl(): String =
        "${repoConfig.apiUrl}/repos/${repoConfig.repository}/milestones"
}

data class GitHubMilestone(val number: Int, val title: String, val htmlUrl: String) {
    companion object {
        fun from(responseContent: Map<String, Any>): GitHubMilestone {
            val number = responseContent["number"] as Int
            val title = responseContent["title"] as String
            val htmlUrl = responseContent["html_url"] as String
            return GitHubMilestone(number, title, htmlUrl)
        }
    }
}
