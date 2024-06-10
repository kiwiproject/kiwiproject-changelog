#!/bin/bash

# Installation/upgrade script for kiwiproject-changelog

# Exit on any error
set -e

# Get the directory where this script lives
script_dir="$(cd "$(dirname "$0")" && pwd)"

# Set default values
install_dir=$HOME/kiwiproject-changelog-script
changelog_script_name=.generate-kiwi-changelog

# Gather and report on configuration options
# TODO arguments, help, etc.

# Start the installation process
echo "ℹ️️  Using installation directory: ${install_dir}"

# TODO Determine if install or upgrade and echo whether we are upgrading or installing for first time
mkdir -p "$install_dir"

echo "ℹ️  Using script name: ${changelog_script_name}"

# Start installation
echo "ℹ️️  Installing kiwiproject-changelog generator"

# Create a working directory to clone and build the JAR
temp_dir=$(mktemp -d)
echo "⚙️  Created temporary directory: $temp_dir"
pushd "$temp_dir" > /dev/null || exit 1

# Clone the repository
echo "⚙️  Cloning latest kiwiproject-changelog"
git clone -q git@github.com:kiwiproject/kiwiproject-changelog.git
pushd kiwiproject-changelog > /dev/null || exit 1

# Switch to the latest release version
latest_tag=$(git for-each-ref --sort=-taggerdate --format '%(refname:strip=2)' refs/tags | head -1)
echo "ℹ️️  Using latest release: ${latest_tag}"
git switch --detach "${latest_tag}"

# Build the executable JAR
echo "⚙️  Building the shaded JAR"
mvn -q package -DskipTests -Pshaded-jar
version="${latest_tag:1}"
jar_file="target/changelog-generator-${version}.jar"
check_jar_file=$(ls "$jar_file")
if [ "$jar_file" != "$check_jar_file" ]; then
  echo "🛑  Expected to find JAR file ${jar_file} but did not find it!"
  exit 1
fi
echo "🚀  Built JAR: $jar_file"

# Copy files to the installation directory
echo "📄  Copy files to installation directory"
changelog_script_path="${install_dir}/${changelog_script_name}"
cp "$script_dir/README.txt" "$install_dir"
cp "$script_dir/generate-kiwi-changelog.sh" "$changelog_script_path"
cp "$jar_file" "${install_dir}/kiwi-changelog-generator.jar"

# Ensure the changelog script is executable
echo "⚙️  Ensure changelog script is executable"
chmod +x "$changelog_script_path"

# Go back to where we started, then remove the temp dir
popd > /dev/null || exit 1  # pop out of kiwiproject-changelog
popd > /dev/null || exit 1  # pop out of the temporary directory

echo "⚙️  Removing temporary directory: ${temp_dir}"
rm -rf "$temp_dir"

# Print some notes about the installation
echo "🍻 Installation complete."
echo
echo "🗒️  Notes:"
echo
echo "✅ Add ${install_dir} to your PATH for the current terminal session using:"
echo
echo "export PATH=\$PATH:${install_dir}"
echo
echo "✅ Or, to use in all terminal sessions, add the following to .bash_profile, .zprofile, etc.:"
echo
echo "# Added by kiwiproject-changelog"
echo "export PATH=\$PATH:${install_dir}"
echo
echo "✅ Execute the changelog script's help command using:"
echo "${changelog_script_path} -h"
echo
echo "Or if you added the changelog script directory to PATH:"
echo "${changelog_script_name} -h"
echo
echo "✅ Confirm the version using:"
echo "${changelog_script_path} -V"
echo
echo "Or if you added the changelog script directory to PATH:"
echo "${changelog_script_name} -h"
echo
