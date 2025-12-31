# Kiwiproject Changelog Generator

[![build](https://github.com/kiwiproject/kiwiproject-changelog/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/kiwiproject/kiwiproject-changelog/actions/workflows/build.yml?query=branch%3Amain)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_kiwiproject-changelog&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=kiwiproject_kiwiproject-changelog)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_kiwiproject-changelog&metric=coverage)](https://sonarcloud.io/summary/new_code?id=kiwiproject_kiwiproject-changelog)
[![CodeQL](https://github.com/kiwiproject/kiwiproject-changelog/actions/workflows/codeql.yml/badge.svg)](https://github.com/kiwiproject/kiwiproject-changelog/actions/workflows/codeql.yml)
[![javadoc](https://javadoc.io/badge2/org.kiwiproject/changelog-generator/javadoc.svg)](https://javadoc.io/doc/org.kiwiproject/changelog-generator)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://img.shields.io/maven-central/v/org.kiwiproject/changelog-generator)](https://central.sonatype.com/artifact/org.kiwiproject/changelog-generator/)

Generates changelogs between two versions on GitHub.

## Installation

You can install using an installation script or by manually building the JAR. The following sections describe these options.

_For both installation types, the script assumes that Maven is available since it is required to build the JAR file._

### Install directly from GitHub using curl

This is the easiest way to install, but of course you should verify that the script is not malicious first. 🤣

To install using the default directory (`~/kiwiproject-changelog-script`) and script name (`.generate-kiwi-changelog`):

```shell
curl -s  https://raw.githubusercontent.com/kiwiproject/kiwiproject-changelog/main/etc/install/install.sh | bash
```

Or, to install to a custom directory and/or with a custom script name:

```shell
curl -s  https://raw.githubusercontent.com/kiwiproject/kiwiproject-changelog/main/etc/install/install.sh | bash -s -- -d <dir> -n <script-name>
```

Commands that require user confirmation (such as when uninstalling or if you attempt to update an existing installation)
work using a `curl`-based installation, but you must use the `-y` (yes) option to avoid interactive prompts and
auto-confirm.

To uninstall, you can:

```shell
curl -s  https://raw.githubusercontent.com/kiwiproject/kiwiproject-changelog/main/etc/install/install.sh | bash -s -- -u -y
```

To install the latest version and overwrite an existing installation, run the original curl command again with `-y`:

```shell
curl -s  https://raw.githubusercontent.com/kiwiproject/kiwiproject-changelog/main/etc/install/install.sh | bash -s -- -y
```

### Execute the installation script locally

Clone the repository, then navigate to the folder where you cloned it.

From the project root directory, you can install using the default location and script name by doing:

```shell
./etc/install/install.sh
```

This will create a directory `~/kiwiproject-changelog-script` with a script named `.generate-kiwi-changelog` inside.
There is also a `README.txt` and the JAR file which is executed by the script.

You can also run `install.sh` with `-h` to get the command line options.

If you want to install to a custom location and/or change the script name, you can do something like:

```shell
./etc/install/install.sh -d /data/tools/kiwi-changelog -n gkc
```

This will install in the `/data/tools/kiwi-changelog` directory, creating it is necessary, and the script is named `gkc`.

You can also _uninstall_ using the `-u` flag. You will be prompted for confirmation, which lets you review the script directory and name are correct.

### Building and running the shaded JAR

If you prefer a manual installation, first clone the repository.

To run the changelog generator from the command line,
you need to build the _shaded_ JAR, which includes all dependencies.

The default Maven build does not include dependencies, so you need to run the build using the `shaded-jar` profile:

```bash
$ mvn package -DskipTests -Pshaded-jar
```

This will build the current SNAPSHOT build in the POM. If you want to build a specific version,
maybe the latest production release, check out the corresponding release tag and build the JAR:

```bash
$ git checkout <release-tag>
$ mvn package -DskipTests -Pshaded-jar
```

`release-tag` must be a valid tag, e.g., `v0.6.0`. You can also build from the latest commit, i.e. `HEAD`.

If you built from a tag, make sure to check out the main branch once you've built the JAR, and it's a good idea to move
the JAR somewhere else so that it doesn't get deleted the next time a `mvn clean` happens.

Now you can run the JAR file with no arguments (or `-h` / `--help` ) to get usage information, for example:

```bash
$ java -jar <path-to-jar>/changelog-generator-0.6.0.jar --help
```

## How it works

Changelogs are generated by finding issues and pull requests that are linked to a specific milestone.
The milestone is derived from the revision (tag), which by default is expected to begin with `"v"`.
For example, revision `v1.4.2` corresponds to milestone `1.4.2` in the GitHub repository.

Revisions can also include pre-release or build metadata, for example `v1.0.0-alpha.1` corresponds
to milestone `1.0.0-alpha.1`.

If the repository uses a different tag format, you need to specify the milestone explicitly.
For example, if tags use a format like `rel-1.4.2` then you can still generate the changelog
as long as you specify the milestone.

Both the revision and the previous revision must be specified. The reason is so the
changelog generator can find the unique _commit_ authors between these two revisions 
and list them as contributors to the release. Note specifically that the contributors are
the people who created the commits, not the people who created the issue or merged the pull
request. Note also that issues and pull requests do not include any information about
commits related to them, so the author information can only be obtained from the commits
that occurred during the milestone.

Labels on the issues and pull requests are mapped to a category.
For example, the label "enhancement" might map to the category "Improvements."
And you can also specify an emoji for each category for a nicer-looking changelog.

By default, the release date shown in the generated changelog is the current UTC date and
time at the moment the changelog is generated. If you want the release date to reflect when
the version was actually tagged, you can use the `--use-tag-date-for-release` option. When
specified, the release date is taken from the annotated Git tag associated with the revision.

### How to prevent duplicates in change logs

Because change logs are generated from the issues and pull requests associated with
a milestone, the resulting change log will contain duplicates if an issue _and its
associated pull request_ are both linked to the milestone.

To prevent this, link _either_ the issue to a milestone (this is our preference
for our own repositories), _or_ link the pull request, but _not_ both.

Issues which do not have an associated pull request (was that you who force-pushed 🔥 it
straight to main?) should be linked to a milestone so that they are included in the change log.

Similarly, pull requests that are not associated with an issue, such as ones created by dependabot,
should be linked to a milestone so that they are included in the change log.

## Command line arguments

Here is a sample argument list that generates a changelog for the [kiwi](https://github.com/kiwiproject/kiwi)
project (inside the kiwiproject organization on GitHub) for version 2.5.0.
It creates the changelog comparing from revision v2.4.0 to v2.5.0.

By using the `--use-tag-date-for-release` option, the release date in the
changelog will be the date from the annotated Git tag associated with the revision
instead of the current UTC date and time.

It also maps several labels to categories, e.g., `-m bug:Bugs` maps from the GitHub label `bugs` to the
changelog category "Bugs" (you can also use `--mapping`). The `-O` option (you can also use `--category-order`)
specifies the order of the categories in the generated changelog.


```
--repo-host-token [YOUR_GITHUB_ACCESS_TOKEN]
--repository kiwiproject/kiwi
--previous-rev v2.4.0
--revision v2.5.0
--use-tag-date-for-release
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
```

In the `-m` options, the GitHub label comes first, then a colon, and finally the category name.
When a label or category name contains a space, you'll need to enclose the entire argument in
quotes.

Make sure to use the _exact_ same category name in the `-m` and `-O` arguments.

The changelog produced using the above arguments is
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
```

Make sure to use the same _exact_ category names in the `-m`, `-e`, and `-O` options.

## External configuration

As of version 0.6.0, you can create a `.kiwi-changelog.yml`. Here is a sample configuration
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
```

### Stripping the `v` prefix when creating the next milestone

When using `--create-next-milestone`, the changelog generator strips a leading
`v` from the milestone title by default. For example, creating a milestone
from `v1.8.2` results in a milestone named `1.8.2`.

If you prefer to keep the `v` prefix (for example, to match your tag naming
convention), you can disable this behavior in the external configuration file:

```yaml
stripVPrefixFromNextMilestone: false
```

### Using the Git annotated tag date as the release date

You can use the `useTagDateForRelease` option in the external configuration file
to set the release date in the changelog to the annotated Git tag date:

```yaml
useTagDateForRelease: true
```

When false or unspecified, the current UTC date and time are used.

Setting this to `true` is equivalent to specifying `--use-tag-date-for-release` on the
command line.
           
### Additional configuration examples

Here is another [sample](sample-kiwi-changelog.yml) changelog configuration. This is the one
we are currently using for kiwiproject releases.

Using a configuration file is more convenient when you have a common set of categories. Note that
the default category is specified using the `default` property in _one_ of the categories.
If you specify more than one default category, then the first one is used.

The  `.kiwi-changelog.yml` file is searched for in the following order (relative to the directory
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

## Example usage with a configuration file

If you export `KIWI_CHANGELOG_TOKEN` to your environment, and you use a configuration file, then
generating change logs can be as simple as:

```shell
.generate-kiwi-changelog --repository kiwiproject/kiwiproject-changelog \
    --previous-rev v0.11.0 \
    --revision v0.12.0 \
    --output-type GITHUB \
    --close-milestone \
    --create-next-milestone 0.13.0
```

This generates the changelog for the `kiwiproject-changelog` repository for milestone `0.12.0`.
It posts the changelog to GitHub, closes the `0.12.0` milestone, and creates the next milestone, `0.13.0`.

You can also include a summary at the top of the generated changelog.
This can be provided directly on the command line using `--summary`, or
read from a file using `--summary-file` (for longer text or when using an
editor):

```shell
.generate-kiwi-changelog --repository kiwiproject/kiwiproject-changelog \
    --previous-rev v0.11.0 \
    --revision v0.12.0 \
    --summary-file release-summary.md \
    --output-type GITHUB \
    --close-milestone \
    --create-next-milestone 0.13.0
```

Using short argument names, it can be even shorter:

```shell
.generate-kiwi-changelog -r kiwiproject/kiwiproject-changelog -p v0.11.0 -R v0.12.0 -y release-summary.md -o GITHUB -C -N 0.13.0
```

Happy change log generating! 🤪
