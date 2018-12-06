#!/bin/bash

set -e
set -u
set -o pipefail

OS_NAME=${1:-linux}
RELEASE_ARCHIVE_EXT=${2:-zip}

RELEASE_VERSION=`cat version.txt`
PACKAGE_NAME="record-and-upload-${RELEASE_VERSION}-${OS_NAME}.${RELEASE_ARCHIVE_EXT}"

echo "*** Building a package with ${PACKAGE_NAME} for ${OS_NAME} ***"
source ./scripts/packr-scripts/common-env-variables.sh
cp ${PACKAGE_NAME} scripts/packr-scripts/${OS_NAME}
./scripts/packr-scripts/${OS_NAME}/${OS_NAME}-pack.sh
echo "*** Finished building a package with ${PACKAGE_NAME} for ${OS_NAME} ***"

