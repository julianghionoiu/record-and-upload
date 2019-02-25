#!/bin/bash

set -e
set -u
set -o pipefail

SCRIPT_CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source ${SCRIPT_CURRENT_DIR}/common-functions.sh

mkdir -p localstore

OSARCH=$(getOSArch)

echo "Detected OS: ${OSARCH}"

exitCodeFile=$(mktemp)
results=$( (java -jar ${PROJECT_ROOT_FOLDER}/build/libs/record-and-upload-${OSARCH}-*all.jar --minimum-required-diskspace 100 --config ${PROJECT_ROOT_FOLDER}/.private/aws-test-secrets --store ${PROJECT_ROOT_FOLDER}/build/play && true); echo $? > "${exitCodeFile}" )

exitCode=$(cat ${exitCodeFile})
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" 1>&2
echo " Running test on record and upload app to verify the minimum disk space requirement check (by passing --minimum-required-diskspace 100)"  1>&2
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" 1>&2

if [[ ${exitCode} -ne 0 ]]; then
  if [[ $(echo ${results} | grep "Sorry, you need at least 100GB of free disk space on this volume (or drive)" ) ]]; then
  	echo "Test PASSED"          1>&2
  else
  	echo "	App failed due to other reasons than disk space requirements." 1>&2

  	echo "App execution logs:"  1>&2
  	echo ${results}             1>&2
  fi
else
  echo "Test should NOT have failed with a 0 exit code"                     1>&2

  echo "Please check if the app is run under the expected conditions" 1>&2

  echo "App execution logs:"  1>&2
  echo ${results}             1>&2
fi
