#!/usr/bin/env bash

set -xe

# Run unit tests
./gradlew clean test --info --console=plain

# Build and test native jars
./gradlew shadowJar -PvideoArch=macos --info --console=plain
./gradlew shadowJar -PvideoArch=linux --info --console=plain
./gradlew shadowJar -PvideoArch=windows --info --console=plain
java -jar build/libs/record-and-upload-linux-*.jar --run-self-test