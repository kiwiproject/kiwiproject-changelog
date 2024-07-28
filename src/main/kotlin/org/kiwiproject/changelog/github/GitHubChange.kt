package org.kiwiproject.changelog.github

data class GitHubChange(val number: Int, val title: String, val htmlUrl: String, val category: String) {

    companion object {
        fun from(issue: GitHubIssue, category: String): GitHubChange {
            return GitHubChange(issue.number, issue.title, issue.htmlUrl, category)
        }
    }

    fun asMarkdown() = "$title [(#$number)]($htmlUrl)"
}
