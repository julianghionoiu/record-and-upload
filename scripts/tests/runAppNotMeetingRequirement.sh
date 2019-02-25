#!/bin/bash

set -e
set -u
set -o pipefail

PROJECT_ROOT_FOLDER=$(realpath $(pwd)/../../)

RED='\033[0;31m'
NC='\033[0m' # No Color

ONE_GB=$((1024 * 1024))

MINIMUM_REQUIRED_DISKSPACE_HUMAN_READABLE=1
MINIMUM_REQUIRED_DISKSPACE=$((${MINIMUM_REQUIRED_DISKSPACE_HUMAN_READABLE} * ${ONE_GB}))
AVAILABLE_DISKSPACE=$(df --output=avail $(pwd) | tail -n 1 | awk '{print $1}')   ### Should work on both Linux and MacOS
AVAILABLE_DISKSPACE_HUMAN_READABLE=$((${AVAILABLE_DISKSPACE} / ${ONE_GB}))
echo "Available disk space on '$(pwd)': ${AVAILABLE_DISKSPACE_HUMAN_READABLE}GB"
echo ""

if [[ "${AVAILABLE_DISKSPACE}" -gt "${MINIMUM_REQUIRED_DISKSPACE}" ]]; then
   echo "${RED}Sorry, you need under ${MINIMUM_REQUIRED_DISKSPACE_HUMAN_READABLE}GB of free disk space on this drive, in order for this test to work."
   echo ""
   echo "Please make sure the expected environment is setup on '$(pwd)' and try running the test again.${NC}"
   exit -1
fi

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
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" 1>&2
echo " Running test on record and upload app to verify the minimum disk space requirement check"     1>&2
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" 1>&2

if [[ ${exitCode} -ne 0 ]]; then
  if [[ $(echo ${results} | grep "Sorry, you need at least 1GB of free disk space on this volume (or drive)" ) ]]; then
  	echo "Test PASSED"          1>&2
  else
  	echo "	App failed due to other reasons than disk space requirements." 1>&2

  	echo "App execution logs:"  1>&2
  	echo ${results}             1>&2
  fi
else
  echo "Test should NOT have failed with a 0 exit code"                     1>&2

  echo "Please check if the app is being run under the expected conditions" 1>&2

  echo "App execution logs:"  1>&2
  echo ${results}             1>&2
fi
