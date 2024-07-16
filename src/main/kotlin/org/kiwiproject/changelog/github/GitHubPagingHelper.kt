package org.kiwiproject.changelog.github

import com.google.common.annotations.VisibleForTesting
import org.apache.commons.lang3.StringUtils.abbreviate
import org.kiwiproject.changelog.github.GitHubApi.GitHubResponse

private const val NO_NEXT_PAGE = "none"

class GitHubPagingHelper {

    fun paginate(api: GitHubApi, firstPageUrl: String, responseHandler: (Int, GitHubResponse) -> Unit) {
        var nextPageUrl = firstPageUrl
        var page = 1

        while (hasNextPage(nextPageUrl)) {
            val response = api.get(nextPageUrl)
            checkOkResponse(response)

            responseHandler(page, response)

            nextPageUrl = getNextPageUrl(response.linkHeader)

            ++page
        }
    }

    private fun hasNextPage(nextPageUrl: String): Boolean {
        return NO_NEXT_PAGE != nextPageUrl
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
            return NO_NEXT_PAGE
        }

        // See GitHub API doc : https://docs.github.com/en/rest/using-the-rest-api/using-pagination-in-the-rest-api

        // Example Link header and value:
        // Link: <https://api.github.com/repositories/6207167/issues?access_token=a0a4c0f41c200f7c653323014d6a72a127764e17&state=closed&filter=all&page=2>; rel="next",
        //       <https://api.github.com/repositories/62207167/issues?access_token=a0a4c0f41c200f7c653323014d6a72a127764e17&state=closed&filter=all&page=4>; rel="last"

        val linkRel = linkHeader.split(",")
            .firstOrNull { url -> url.contains("rel=\"next\"") }

        return linkRel?.substring(
            startIndex = linkRel.indexOf("http"),
            endIndex = linkRel.indexOf(">; rel=\"next\"")
        ) ?: NO_NEXT_PAGE
    }
}
