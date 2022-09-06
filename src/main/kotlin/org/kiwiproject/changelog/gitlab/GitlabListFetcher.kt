package org.kiwiproject.changelog.gitlab

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.kiwiproject.changelog.config.RepoHostConfig

class GitlabListFetcher(private val repoHostConfig: RepoHostConfig) {

    // see API doc: https://docs.gitlab.com/ee/api/issues.html
    // Default query params from docs:
    //     state = open
    //     direction = desc
    private var nextPageUrl = "${repoHostConfig.apiUrl}/projects/${repoHostConfig.repository}/issues?state=closed"
    private val mapper = jacksonObjectMapper()

    fun hasNextPage() : Boolean {
        return "none" != nextPageUrl
    }

    fun nextPage(): List<Map<String, Any>> {
        if (!hasNextPage()) {
            throw IllegalStateException("GitLab API has no more issues to fetch. Did you run 'hasNextPage()' method?")
        }

        val api = GitlabApi(repoHostConfig.token)
        val response = api.get(nextPageUrl)

        nextPageUrl = getNextPageUrl(response.linkHeader)

        return mapper.readValue(response.content)
    }

    private fun getNextPageUrl(linkHeader: String?) : String {
        if (linkHeader == null) {
            return "none"
        }

        // TODO @chrisrohr the comment below references the GitHub documentation, not GitLab. Does this need
        //  to change for GitLab? Looks like this was due to copying from GithubListFetcher.
        // See GitHub API doc : https://developer.github.com/guides/traversing-with-pagination/
        // Link: <https://api.github.com/repositories/6207167/issues?access_token=a0a4c0f41c200f7c653323014d6a72a127764e17&state=closed&filter=all&page=2>; rel="next",
        //       <https://api.github.com/repositories/62207167/issues?access_token=a0a4c0f41c200f7c653323014d6a72a127764e17&state=closed&filter=all&page=4>; rel="last"
        val linkRel = linkHeader.split(",")
            .firstOrNull { url -> url.contains("rel=\"next\"") }

        return linkRel?.substring(linkRel.indexOf("http"), linkRel.indexOf(">; rel=\"next\"")) ?: "none"
    }
}
