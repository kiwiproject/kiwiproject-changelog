# Kiwiproject Changelog Generator

[![build](https://github.com/kiwiproject/kiwiproject-changelog/actions/workflows/build.yml/badge.svg)](https://github.com/kiwiproject/kiwiproject-changelog/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_kiwiproject-changelog&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=kiwiproject_kiwiproject-changelog)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_kiwiproject-changelog&metric=coverage)](https://sonarcloud.io/summary/new_code?id=kiwiproject_kiwiproject-changelog)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

Generates changelogs between two versions on GitHub.

Here is a sample argument list that generates a changelog for the [kiwi](https://github.com/kiwiproject/kiwi) 
project (inside the kiwiproject organization on GitHub) for version 2.5.0.
It creates the changelog comparing from revision v2.4.0 to v2.5.0 using the given working directory.
It also maps several labels to categories, e.g., `-m bug:Bugs`
maps from the GitHub label `bugs` to the changelog category "Bugs" (you can also use `--mapping`).
The `--include-prs-from` (or `-i`) lets you include PRs from specific users, e.g. a bot like dependabot.
And finally, it uses the `-O` option (you can also use `--category-order`) to specify the order of
the categories in the generated changelog.

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

In the `-m` options, the GitHub label comes first, then a colon, and finally the category name.
When a label or category name contains a space, you'll need to enclose the entire argument in
quotes.

The changelog that was produced using the above arguments is
[here](https://github.com/kiwiproject/kiwi/releases/tag/v2.5.0).

Note that you need a [GitHub personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)
that has appropriate access to be able to read issues in the repository. 
You can supply this token using the command line (as shown above), or by setting an environment variable
named `KIWI_CHANGELOG_TOKEN`.

You can also add some 🌶️ to your change logs using emoji.
In label-to category mappings and category order arguments, add emoji.
For example:

```
-m "bug:Bugs 🐛"
-m "new feature:Improvements 🚀"
-m "enhancement:Improvements 🚀"
-m "dependencies:Dependency Updates ⬆️"
-m "code cleanup:Assorted 👜"
-m "refactoring:Assorted 👜"
--include-prs-from dependabot[bot]
-O "Improvements 🚀"
-O "Bugs 🐛"
-O "Assorted 👜"
-O "Dependency Updates ⬆️"
```

Make sure to use the same _exact_ same category names so that they are grouped together.
