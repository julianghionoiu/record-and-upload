#!/bin/bash

if grep "SNAPSHOT" version.txt; then
    printf 'Not on release branch. Stopping.\n' >&2
    exit 1
fi

if [ -z "${GITHUB_TOKEN}" ]; then
    printf 'GITHUB_TOKEN environment variable not set. Stopping.\n' >&2
    exit 1;
fi

RELEASE_VERSION=`cat version.txt`
TAG_NAME="v${RELEASE_VERSION}"
POST_DATA=`printf '{
  "tag_name": "%s",
  "target_commitish": "master",
  "name": "%s",
  "body": "Release %s",
  "draft": false,
  "prerelease": false
}' ${TAG_NAME} ${TAG_NAME} ${TAG_NAME}`
echo "Creating release ${RELEASE_VERSION}: $POST_DATA"
curl \
    -H "Authorization: token ${GITHUB_TOKEN}" \
    -H "Content-Type: application/json" \
    -H "Accept: application/vnd.github.v3+json" \
    -X POST -d "${POST_DATA}" "https://api.github.com/repos/julianghionoiu/record-and-upload/releases"


CURL_OUTPUT="./build/github-release.listing"
echo "Getting Github ReleaseId"
curl \
    -H "Authorization: token ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github.v3+json" \
    -X GET "https://api.github.com/repos/julianghionoiu/record-and-upload/releases/tags/${TAG_NAME}" |
    tee ./build/github-release.listing
RELEASE_ID=`cat ${CURL_OUTPUT} | grep id | head -n 1 | tr -d " " | tr "," ":" | cut -d ":" -f 2`


## TODO: should we ever upload a vanilla uber jar without its OS dependency in it?
OS_NAME=${1:-linux}
PACKAGE_NAME="record-and-upload-${RELEASE_VERSION}-${OS_NAME}.jar"
RELEASE_JAR="./build/libs/${PACKAGE_NAME}"
echo "Uploading asset to ReleaseId ${RELEASE_ID}, name=$PACKAGE_NAME"
curl \
    -H "Authorization: token ${GITHUB_TOKEN}" \
    -H "Content-Type: application/zip" \
    -H "Accept: application/vnd.github.v3+json" \
    --data-binary @${RELEASE_JAR} \
     "https://uploads.github.com/repos/julianghionoiu/record-and-upload/releases/${RELEASE_ID}/assets?name=${PACKAGE_NAME}"

source ./scripts/packr-scripts/common-env-variables.sh
if [[ "${OS_NAME}"=="linux" ]]; then
   cp ${PACKAGE_NAME} scripts/packr-scripts/linux
   ./scripts/packr-scripts/linux/linux-pack.sh
fi

## Pushing the Linux version of the record-and-upload zip file to github
PACKAGE_NAME="record-and-upload-${RELEASE_VERSION}-${OS_NAME}.zip"
RELEASE_ZIP="./packr-scripts/linux/${PACKAGE_NAME}"
echo "Uploading asset to ReleaseId ${RELEASE_ID}, name=$PACKAGE_NAME"
curl \
    -H "Authorization: token ${GITHUB_TOKEN}" \
    -H "Content-Type: application/zip" \
    -H "Accept: application/vnd.github.v3+json" \
    --data-binary @${RELEASE_ZIP} \
     "https://uploads.github.com/repos/julianghionoiu/record-and-upload/releases/${RELEASE_ID}/assets?name=${PACKAGE_NAME}"