#!/bin/bash

set -e
set -u
set -o pipefail

PROJECT_ROOT_FOLDER=$(realpath $(pwd)/../../)

mkdir -p localstore

echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
echo " Running record-and-upload app by setting the RECORD_AND_UPLOAD_MINIMUM_DISKSPACE to a higher than the free disk space to simulate a failure"
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"

OSARCH="linux"
case "$(uname)" in
  CYGWIN* )
    OSARCH="windows"
    ;;

  Darwin* )
    OSARCH="macos"
    ;;

  MINGW* )
    OSARCH="windows"
    ;;

esac

#set RECORD_AND_UPLOAD_MINIMUM_DISKSPACE=100
echo "Detected OS: ${OSARCH}"

java -jar ${PROJECT_ROOT_FOLDER}/build/libs/record-and-upload-${OSARCH}-*all.jar --config ${PROJECT_ROOT_FOLDER}/.private/aws-test-secrets --store ${PROJECT_ROOT_FOLDER}/build/play

#unset set RECORD_AND_UPLOAD_MINIMUM_DISKSPACE