#!/bin/sh
# Builds the project from any working directory, also when run through a symlink.
# JDK 17 is selected by the Java toolchain (build.gradle) and gradle/gradle-daemon-jvm.properties
src="$0"
while [ -h "$src" ]; do
    link="$(readlink "$src")"
    case "$link" in
        /*) src="$link" ;;
        *) src="$(dirname "$src")/$link" ;;
    esac
done
dir="$(cd "$(dirname "$src")" && pwd)" || exit 1
exec "$dir/gradlew" --project-dir "$dir" build "$@"
