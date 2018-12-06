#!/bin/bash

set -e
set -u
set -o pipefail

OS_NAME=${1:-linux}
RELEASE_ARCHIVE_EXT=${2:-tgz}

RELEASE_VERSION=`cat version.txt`
SCRIPT_DIR=$(dirname $0)
PARENT_DIR=$(realpath ${SCRIPT_DIR}/..)
PATH_TO_JAR=${PARENT_DIR}/build/libs
JAR_FILE_NAME=$(ls ${PATH_TO_JAR}/record-and-upload-${OS_NAME}-${RELEASE_VERSION}*.jar || true)

if [[ -z "${JAR_FILE_NAME}" ]]; then
   echo "Cannot find the ${OS_NAME} specific jar file in ${PATH_TO_JAR} folder, please check if it has been built"
   exit -1
fi

NEW_JAR_FILE_NAME="record-and-upload-${OS_NAME}-${RELEASE_VERSION}.jar"
PACKAGE_NAME="record-and-upload-${OS_NAME}-${RELEASE_VERSION}.${RELEASE_ARCHIVE_EXT}"

echo "*** Building package ${PACKAGE_NAME} ***"
source ${SCRIPT_DIR}/packr-scripts/common-env-variables.sh

echo "Removing old ${PACKAGE_NAME} file(s)"
rm ${SCRIPT_DIR}/packr-scripts/${OS_NAME}/*.jar

echo "Copying ${PACKAGE_NAME} into the ${SCRIPT_DIR}/packr-scripts/${OS_NAME}/ folder"
cp ${JAR_FILE_NAME} ${SCRIPT_DIR}/packr-scripts/${OS_NAME}/${NEW_JAR_FILE_NAME}

${SCRIPT_DIR}/packr-scripts/${OS_NAME}/${OS_NAME}-pack.sh

if [[ -s ${SCRIPT_DIR}/packr-scripts/${OS_NAME}/${PACKAGE_NAME} ]]; then
  echo "*** Finished building package ${PACKAGE_NAME} ***"
else
  echo "*** Failed to build ${PACKAGE_NAME} ***"
  exit -1
fi
