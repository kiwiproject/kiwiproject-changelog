# Kiwiproject Changelog Generator

[![build](https://github.com/kiwiproject/kiwiproject-changelog/actions/workflows/build.yml/badge.svg)](https://github.com/kiwiproject/kiwiproject-changelog/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

Generates changelogs between two versions on GitHub or GitLab.  The default options assume GitHub.

Here is a sample argument list that generates a changelog for the kiwi project (inside the kiwiproject organization on GitHub) for version 2.5.0. It creates the changelog comparing from revision v2.4.0 to v2.5.0 using the given working directory. It also maps several labels to categories, e.g. `-m bug:Bugs` maps from the GitHub label `bugs` to the changelog category "Bugs" (you can also use `--mapping`). The `--include-prs-from` (or `-i`) lets you include PRs from specific users, e.g. a bot like dependabot. And finally, it uses the `-O` option (you can also use `--category-order`) to specify the order of the categories in the generated changelog.

```
--repo-host-token [YOUR_GITHUB_ACCESS_TOKEN]
--repository kiwiproject/kiwi
--previous-rev v2.4.0
--revision v2.5.0
--working-dir /home/users/bob/projects/kiwiproject/kiwi
-m bug:Bugs
-m "new feature:Improvements"
-m enhancement:Improvements
-m "dependencies:Dependency Updates"
-m "code cleanup:Assorted"
-m refactoring:Assorted
--include-prs-from dependabot[bot]
-O Improvements
-O Bugs
-O Assorted
-O "Dependency Updates"
```

The changelog produced using the above resulted in [this](https://github.com/kiwiproject/kiwi/releases/tag/v2.5.0) output.

Note that you need a GitHub or GitLab personal access token that has appropriate access to be able to read issues in the repository.
