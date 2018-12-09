#!/bin/bash

RECORD_AND_UPLOAD_JAR="../record-and-upload.jar"
PACKAGE_NAME=record-and-upload
EXE_NAME=record-and-upload
PACKR_TARGET_FOLDER=record

MAINTAINER_NAME="Accelerate.io"
PACKAGE_DESCRIPTION="..."
VENDOR="${MAINTAINER_NAME}"
PRODUCT_URL="https://www.accelerate.io/"
VERSION="1.0"
LICENSE_NAME="OPEN/PUBLIC"

getArchiveName(){
	OS_NAME=""
	if [[ ! -z "${1:-}" ]]; then
	   OS_NAME="${1}"
	fi

	ARCHIVE_EXTENSION="zip"
	if [[ ! -z "${2:-}" ]]; then
	   ARCHIVE_EXTENSION="${2}"
	fi

	echo "record-and-upload-${OS_NAME}.${ARCHIVE_EXTENSION}"
}

ZIP_ARCHIVE_NAME=$(getArchiveName zip)
TGZ_ARCHIVE_NAME=$(getArchiveName tgz)

download_os_specific_jre_archive() {
	JRE_ZIP_FILE_NAME=${1}
	JRE_ARCHIVE_URL=${2}
	if [[ ! -s ${JRE_ZIP_FILE_NAME} ]]; then
	   echo "Downloading JRE for ${OS_NAME} from ${JRE_ARCHIVE_URL}"
	   wget ${JRE_ARCHIVE_URL}
	fi

	if [[ ! -s ${JRE_ZIP_FILE_NAME} ]]; then
	   echo "JRE for ${OS_NAME} was not found, please place one of them in the current directory and try running the script again."
	   echo "It is possible ${JRE_ARCHIVE_URL} does not contain the expected archive needed for the rest of the process"
	   echo "Process halted."
	   exit -1
	fi
}