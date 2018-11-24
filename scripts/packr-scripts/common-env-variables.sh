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

getZipArchiveName(){
	OS_NAME=""
	if [[ ! -z "${1:-}" ]]; then
	   OS_NAME="-${1}"
	fi
	echo "record-and-upload-${OS_NAME}.zip"
}
ZIP_ARCHIVE_NAME=$(getZipArchiveName)