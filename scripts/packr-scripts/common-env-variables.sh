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
