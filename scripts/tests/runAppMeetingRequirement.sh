#!/bin/bash

set -e
set -u
set -o pipefail

PROJECT_ROOT_FOLDER=$(realpath $(pwd)/../../)

mkdir -p localstore

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

echo "Detected OS: ${OSARCH}"

exitCodeFile=$(mktemp)
results=$( (java -jar ${PROJECT_ROOT_FOLDER}/build/libs/record-and-upload-${OSARCH}-*all.jar --config ${PROJECT_ROOT_FOLDER}/.private/aws-test-secrets --store ${PROJECT_ROOT_FOLDER}/build/play && true); echo $? > "${exitCodeFile}" )

exitCode=$(cat ${exitCodeFile})
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" 1>&2
echo " Running test on record and upload app to verify happy path when diskspace is available and the minimum required diskspace param is not passed)"  1>&2
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" 1>&2

if [[ ${exitCode} -eq 0 ]]; then
  if [[ $(echo ${results} | grep "Start S3 Sync session" ) ]]; then
  	echo "Test PASSED"          1>&2
  else
  	echo "	App failed due to other reasons than disk space requirements." 1>&2

  	echo "App execution logs:"  1>&2
  	echo ${results}             1>&2
  fi
else
  echo "Test should NOT have failed with a non-zero exit code"        1>&2

  echo "Please check if the app is run under the expected conditions" 1>&2

  echo "App execution logs:"  1>&2
  echo ${results}             1>&2
fi
