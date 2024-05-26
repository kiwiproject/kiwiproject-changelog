package org.kiwiproject.changelog.github

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.annotations.VisibleForTesting
import org.apache.commons.lang3.StringUtils.abbreviate
import org.kiwiproject.changelog.config.RepoHostConfig
import org.kiwiproject.changelog.github.GithubApi.GitHubResponse

class GithubListFetcher(private val repoHostConfig: RepoHostConfig) {

    // see API doc: https://developer.github.com/v3/issues/
    // Default query params from docs:
    //     per_page = 30
    //     state = open
    //     filter = all
    //     direction = desc
    private var nextPageUrl = "${repoHostConfig.apiUrl}/repos/${repoHostConfig.repository}/issues?page=1&per_page=100&state=closed&filter=all&direction=desc"
    private val mapper = jacksonObjectMapper()

    fun hasNextPage() : Boolean {
        return "none" != nextPageUrl
    }

    fun nextPage(): List<Map<String, Any>> {
        check(hasNextPage()) { "GitHub API has no more issues to fetch. Did you run 'hasNextPage()' method?" }

        val api = GithubApi(repoHostConfig.token)
        val response = api.get(nextPageUrl)
        checkOkResponse(response)

        nextPageUrl = getNextPageUrl(response.linkHeader)

        return mapper.readValue(response.content)
    }

    @VisibleForTesting
    fun checkOkResponse(response: GitHubResponse) {
        check(response.statusCode == 200) {
            val truncatedContent = abbreviate(response.content, 100)
            "GET ${response.requestUri} failed, response code ${response.statusCode}, response body\n:$truncatedContent"
        }
    }

    @VisibleForTesting
    fun getNextPageUrl(linkHeader: String?) : String {
        if (linkHeader == null) {
            return "none"
        }

        // See GitHub API doc : https://docs.github.com/en/rest/using-the-rest-api/using-pagination-in-the-rest-api

        // Example Link header and value:
        // Link: <https://api.github.com/repositories/6207167/issues?access_token=a0a4c0f41c200f7c653323014d6a72a127764e17&state=closed&filter=all&page=2>; rel="next",
        //       <https://api.github.com/repositories/62207167/issues?access_token=a0a4c0f41c200f7c653323014d6a72a127764e17&state=closed&filter=all&page=4>; rel="last"

        val linkRel = linkHeader.split(",")
            .firstOrNull { url -> url.contains("rel=\"next\"") }

        return linkRel?.substring(linkRel.indexOf("http"), linkRel.indexOf(">; rel=\"next\"")) ?: "none"
    }
}
