#!/bin/bash

OS_NAME=${1:-linux}
RELEASE_VERSION=${2}
RELEASE_ID=${3}

upload_os_specific_jar() {
  PACKAGE_NAME="record-and-upload-${OS_NAME}-${RELEASE_VERSION}.jar"
  RELEASE_JAR="./build/libs/${PACKAGE_NAME}"
  echo "Uploading asset to ReleaseId ${RELEASE_ID}, name=$PACKAGE_NAME"
  curl \
      -H "Authorization: token ${GITHUB_TOKEN}" \
      -H "Content-Type: application/zip" \
      -H "Accept: application/vnd.github.v3+json" \
      --data-binary @${RELEASE_JAR} \
       "https://uploads.github.com/repos/julianghionoiu/record-and-upload/releases/${RELEASE_ID}/assets?name=${PACKAGE_NAME}"
}
