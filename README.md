# Kiwiproject Changelog Generator

Generates changelogs between two versions on GitHub or GitLab.  The default options assume GitHub.

Here is a sample argument list that generates a changelog for the kiwi project (inside the kiwiproject organization on GitHub) for version v0.22.0. It creates the changelog comparing from revision v0.21.0 to v0.22.0 using the given working directory. It also maps several labels to categories, e.g. `-m bug:Bugs` maps from the GitHub label `bugs` to the changelog category "Bugs". And finally, it uses the `-O` option (you can also use `--category-order`) to specify the order of the categories in the generated changelog.

```
--repo-host-token [YOUR_GITHUB_ACCESS_TOKEN]
--repository kiwiproject/kiwi
--previous-rev v1.1.8
--revision v1.1.9
--working-dir /home/users/bob/projects/kiwiproject/kiwi
--mapping bug:Bugs
--mapping "new feature:Improvements"
--mapping enhancement:Improvements
--mapping "dependencies:Dependency Updates"
--mapping "code cleanup:Assorted"
--mapping refactoring:Assorted
--include-prs-from dependabot[bot]
--category-order Improvements
--category-order Bugs
--category-order Assorted
--category-order "Dependency Updates"
```

The changelog produced using the above resulted in [this](https://github.com/kiwiproject/kiwi/releases/tag/v0.22.0) output.

Note that you need a GitHub or GitLab personal access token that has appropriate access to be able to read issues in the repository.
