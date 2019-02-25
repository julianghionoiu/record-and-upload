#!/bin/bash

set -e
set -u
set -o pipefail

export PROJECT_ROOT_FOLDER=$(realpath ${SCRIPT_CURRENT_DIR}/../../)

export RED='\033[0;31m'
export NC='\033[0m' # No Color

export ONE_GB=$((1024 * 1024))

export MINIMUM_REQUIRED_DISKSPACE_HUMAN_READABLE=1
export MINIMUM_REQUIRED_DISKSPACE=$((${MINIMUM_REQUIRED_DISKSPACE_HUMAN_READABLE} * ${ONE_GB}))
export AVAILABLE_DISKSPACE=$(df --output=avail $(pwd) | tail -n 1 | awk '{print $1}')   ### Should work on both Linux and MacOS
export AVAILABLE_DISKSPACE_HUMAN_READABLE=$((${AVAILABLE_DISKSPACE} / ${ONE_GB}))
echo "Available disk space on '$(pwd)': ${AVAILABLE_DISKSPACE_HUMAN_READABLE}GB"
echo ""

function getOSArch() {
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

    echo ${OSARCH}
}
