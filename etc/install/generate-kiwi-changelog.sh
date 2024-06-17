#!/bin/sh

# This script executes the kiwiproject-changelog JAR file.
# It was installed using the etc/install/install.sh script
# in the kiwiproject-changelog repository.

# Get the directory of the script
script_dir="$(cd "$(dirname "$0")" && pwd)"

# Execute the JAR, passing through all arguments
java -jar "${script_dir}/kiwi-changelog-generator.jar" "$@"
