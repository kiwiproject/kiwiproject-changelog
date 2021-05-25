# Kiwiproject Changelog Generator

Generates changelogs between two versions on GitHub.

Here is a sample argument list that generates a changelog for the kiwi project (inside the kiwiproject organization) for version v0.22.0. It creates the changelog comparing from revision v0.21.0 to v0.22.0 using the given working directory. It also maps several labels to categories, e.g. `-m bug:Bugs` maps from the GitHub label `bugs` to the changelog category "Bugs". And finally, it uses the `-O` option (you can also use `--category-order`) to specify the order of the categories in the generated changelog.

```
--github-repository kiwiproject/kiwi
--github-token [YOUR_GITHUB_ACCESS_TOKEN]
--previous-rev v0.21.0
--rev v0.22.0
--version v0.22.0
--working-dir /home/users/bob/projects/kiwiproject/kiwi
-m bug:Bugs
-m "code cleanup:Assorted"
-m "new feature:Improvements"
-m enhancement:Improvements
-m "dependencies:Dependency Updates"
-i dependabot[bot]
-O Improvements
-O Bugs
-O Assorted
-O "Dependency Updates"
```

The changelog produced using the above resulted in [this](https://github.com/kiwiproject/kiwi/releases/tag/v0.22.0) output.

Note that you need a GitHub personal access token that has appropriate access to be able to read issues in the repository.
