#!/bin/sh

# This script executes the kiwiproject-changelog JAR file.
# It was installed using the etc/install/install.sh script
# in the kiwiproject-changelog repository.

# Get the directory of the script
script_dir="$(cd "$(dirname "$0")" && pwd)"

# Use custom Logback config if present in ~/.kiwi-changelog/
logback_opt=""
if [ -f "$HOME/.kiwi-changelog/logback.xml" ]; then
    logback_opt="-Dlogback.configurationFile=$HOME/.kiwi-changelog/logback.xml"
    echo "ℹ️  Using custom Logback config: $HOME/.kiwi-changelog/logback.xml"
fi

# Execute the JAR, passing through all arguments
if [ -n "$logback_opt" ]; then
    java "$logback_opt" -jar "${script_dir}/kiwi-changelog-generator.jar" "$@"
else
    java -jar "${script_dir}/kiwi-changelog-generator.jar" "$@"
fi
