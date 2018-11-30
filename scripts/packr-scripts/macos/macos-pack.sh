#!/bin/bash

set -e
set -u
set -o pipefail

source ../common-env-variables.sh

JRE_ZIP_FILE_NAME=$(ls jre*.zip) #jre1.8.0_101-osx.zip
RELEASE_VERSION=$(git describe --abbrev=0 --tags)
TGZ_ARCHIVE_NAME=$(getArchiveName "${RELEASE_VERSION}-macos" "tgz")
RECORD_AND_UPLOAD_JAR=${PACKAGE_NAME}-${RELEASE_VERSION}.jar

if [[ ! -s ${RECORD_AND_UPLOAD_JAR} ]]; then
   echo "Jar file ${RECORD_AND_UPLOAD_JAR} not found"
   echo "Downloading ${RECORD_AND_UPLOAD_JAR} from github"
   wget https://github.com/julianghionoiu/record-and-upload/releases/download/${RELEASE_VERSION}/${RECORD_AND_UPLOAD_JAR} ${PACKAGE_NAME}
fi

echo 
echo "*** Removing the old image folders: ${PACKR_TARGET_FOLDER} ***"
rm -fr ${PACKR_TARGET_FOLDER} || true

echo
echo "*** Removing the old ${TGZ_ARCHIVE_NAME} archive ***"
rm -fr ${TGZ_ARCHIVE_NAME} || true

time java -jar ../packr.jar \
     --platform mac \
     --executable ${EXE_NAME} \
     --classpath ${RECORD_AND_UPLOAD_JAR} \
     --jdk ${JRE_ZIP_FILE_NAME} \
     --mainclass tdl.record_upload.RecordAndUploadApp \
     --vmargs Xmx2G \
     --minimizejre ../reduced-jre.json \
     --output ${PACKR_TARGET_FOLDER}

HUMBLE_MACOS_LIB=libhumblevideo.dylib
echo "*** Uncompressing  ${HUMBLE_MACOS_LIB} from ${RECORD_AND_UPLOAD_JAR} into '${PACKR_TARGET_FOLDER}' ***"
time unzip -o ${PACKR_TARGET_FOLDER}/Contents/Resources/${RECORD_AND_UPLOAD_JAR} ${HUMBLE_MACOS_LIB}
mv ${HUMBLE_MACOS_LIB} ${PACKR_TARGET_FOLDER}/Contents/MacOS/

echo "*** Removing ${HUMBLE_MACOS_LIB} from ${RECORD_AND_UPLOAD_JAR} in '${PACKR_TARGET_FOLDER}' ***"
time zip -d ${PACKR_TARGET_FOLDER}/Contents/Resources/${RECORD_AND_UPLOAD_JAR} ${HUMBLE_MACOS_LIB}

time tar -czvf ${TGZ_ARCHIVE_NAME} ${PACKR_TARGET_FOLDER}