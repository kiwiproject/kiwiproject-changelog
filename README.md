# Kiwiproject Changelog Generator

[![build](https://github.com/kiwiproject/kiwiproject-changelog/actions/workflows/build.yml/badge.svg)](https://github.com/kiwiproject/kiwiproject-changelog/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_kiwiproject-changelog&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=kiwiproject_kiwiproject-changelog)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_kiwiproject-changelog&metric=coverage)](https://sonarcloud.io/summary/new_code?id=kiwiproject_kiwiproject-changelog)
[![CodeQL](https://github.com/kiwiproject/kiwiproject-changelog/actions/workflows/codeql.yml/badge.svg)](https://github.com/kiwiproject/kiwiproject-changelog/actions/workflows/codeql.yml)
[![javadoc](https://javadoc.io/badge2/org.kiwiproject/changelog-generator/javadoc.svg)](https://javadoc.io/doc/org.kiwiproject/changelog-generator)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://img.shields.io/maven-central/v/org.kiwiproject/changelog-generator)](https://central.sonatype.com/artifact/org.kiwiproject/changelog-generator/)

Generates changelogs between two versions on GitHub.

## Building and running the shaded JAR

In order to run the changelog generator from the command line, you need to build the _shaded_ JAR which includes all dependencies.

The default Maven build does not include dependencies, so you need to run the build using the `shaded-jar` profile:

```bash
$ mvn package -DskipTests -Pshaded-jar
```

This will build the current SNAPSHOT build in the POM. If you want to build a specific version,
for example the latest production release, just check out the corresponding release tag build the JAR:

```bash
$ git checkout <release-tag>
$ mvn package -DskipTests -Pshaded-jar
```

where `release-tag` is a valid tag, e.g., `v0.6.0`.

Make sure to check out the main branch once you've built the JAR, and maybe move the JAR somewhere else
so that it doesn't get deleted the next time a `mvn clean` happens.

Now you can run the JAR file with no arguments (or `-h` / `--help` ) to get usage information, for example:

```bash
$ java -jar <path-to-jar>/changelog-generator-0.6.0.jar --help
```

## Command line arguments

Here is a sample argument list that generates a changelog for the [kiwi](https://github.com/kiwiproject/kiwi) 
project (inside the kiwiproject organization on GitHub) for version 2.5.0.
It creates the changelog comparing from revision v2.4.0 to v2.5.0 using the given working directory.
It also maps several labels to categories, e.g., `-m bug:Bugs` maps from the GitHub label `bugs` to the
changelog category "Bugs" (you can also use `--mapping`). The `-O` option (you can also use `--category-order`)
specifies the order of the categories in the generated changelog. And last, the `--include-prs-from` (or `-i`)
option lets you include PRs from specific users, e.g. a bot like dependabot.


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
-O Improvements
-O Bugs
-O Assorted
-O "Dependency Updates"
--include-prs-from dependabot[bot]
```

In the `-m` options, the GitHub label comes first, then a colon, and finally the category name.
When a label or category name contains a space, you'll need to enclose the entire argument in
quotes.

Make sure to use the _exact_ same category name in the `-m` and `-O` arguments.

The changelog that was produced using the above arguments is
[here](https://github.com/kiwiproject/kiwi/releases/tag/v2.5.0).

Note that you need a [GitHub personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)
that has appropriate access to be able to read issues in the repository. 
You can supply this token using the command line (as shown above), or by setting an environment variable
named `KIWI_CHANGELOG_TOKEN`.

## Default category

If you don't specify (or don't want to specify) a mapping for every label, then a default
category is assigned. The default category is _Assorted_ with no emoji.

You can specify the default category using the `--default-category` (or `-c`) command
line argument.

## Emoji support

You can also add some 🌶️ to your change logs using emoji.

Use the `-e` or `--emoji-mapping` command line options to specify the emoji to use for a category.
For example:

```
-m "bug:Bugs"
-m "new feature:Improvements"
-m enhancement:Improvements
-m "dependencies:Dependency Updates"
-m "code cleanup:Assorted"
-m refactoring:Assorted
-e "Bugs:🐛"
-e "Improvements:🚀"
-e "Dependency Updates:⬆️"
-e "Assorted:👜"
-O Improvements
-O Bugs
-O Assorted
-O "Dependency Updates"
--include-prs-from dependabot[bot]
```

Make sure to use the same _exact_ same category names in the `-m`, `-e`, and `-O` options.

## External configuration

As of version 0.6.0, you can create a `.kiwiproject-changelog.yml`. Here is a sample configuration
that is equivalent to the above command line arguments:

```yaml
---

categories:

  - name: Improvements
    emoji: 🚀
    labels:
      - new feature
      - enhancement

  - name: Bugs
    emoji: 🐛
    labels:
      - bug

  - name: Assorted
    emoji: 👜
    default: true
    labels:
      - code cleanup
      - refactoring

  - name: Dependency Updates
    emoji: ⬆️
    labels:
      - dependencies

alwaysIncludePRsFrom:
  - dependabot[bot]
```

Using a configuration file is more convenient when you have a common set of categories. Note that
the default category is specified using the `default` property in _one_ of the categories.
If you specify more than one default category, then the first one is used.

The  `.kiwiproject-changelog.yml` file is searched for in the following order (relative to the directory
where the changelog generator JAR is executed):

1. current directory
2. parent directory
3. user's home directory

This order lets you have a custom changelog configuration for a repository, or a group of 
repositories such as an organization, or a configuration to be used for _any_ repository.

If you don't want any `.kiwi-changelog.yml` configuration files to be used at all, you can
use the `--ignore-config-files` (or `-g`) command line flag.

You can also override the above and specify a custom configuration file using the
`--config-file` (or `-n`) command line option, e.g. `--config-file /path/to/custom/changelog.yml`.
