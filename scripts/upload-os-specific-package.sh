#!/bin/bash

OS_NAME=${1:-zip}
RELEASE_ARCHIVE_EXT=${3:-zip}
RELEASE_VERSION=${3}
RELEASE_ID=${4}

upload_os_specific_package() {
  ## Pushing the OS-specific version of the record-and-upload archive file to github releases
  PACKAGE_NAME="record-and-upload-${OS_NAME}-${RELEASE_VERSION}.${RELEASE_ARCHIVE_EXT}"
  RELEASE_ARCHIVE="./packr-scripts/${OS_NAME}/${PACKAGE_NAME}"
  echo "Uploading asset to ReleaseId ${RELEASE_ID}, name=$PACKAGE_NAME"
  curl \
      -H "Authorization: token ${GITHUB_TOKEN}" \
      -H "Content-Type: application/${RELEASE_ARCHIVE_EXT}" \
      -H "Accept: application/vnd.github.v3+json" \
      --data-binary @${RELEASE_ARCHIVE} \
       "https://uploads.github.com/repos/julianghionoiu/record-and-upload/releases/${RELEASE_ID}/assets?name=${PACKAGE_NAME}"  
}
