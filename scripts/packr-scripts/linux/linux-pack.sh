#!/bin/bash

set -e
set -u
set -o pipefail

source ../common-env-variables.sh

JRE_ZIP_FILE_NAME=$(ls jre*.zip) #jre1.8.0_152.zip

if [[ ! -s ${JRE_ZIP_FILE_NAME} ]]; then
   echo "JRE for Linux was not found, please place one of them in the current directory and try running the script again."
   echo "Process halted."
   exit -1
fi

# This would be a wget command that downloads the jar from the record-and-upload git repo
# after the release process has push the artifact to github
# the artifact will contain the OS specific humble video and the uber jar for record-and-upload
TARGET_JAR_FILE=record-and-upload
RELEASE_VERSION=$(git describe --abbrev=0 --tags)
RECORD_AND_UPLOAD_JAR=${TARGET_JAR_FILE}-${RELEASE_VERSION}.jar
if [[ ! -s ${RECORD_AND_UPLOAD_JAR} ]]; then
   echo "Jar file ${RECORD_AND_UPLOAD_JAR} not found"
   echo "Downloading ${RECORD_AND_UPLOAD_JAR} from github"
   wget https://github.com/julianghionoiu/record-and-upload/releases/download/${RELEASE_VERSION}/${RECORD_AND_UPLOAD_JAR} ${TARGET_JAR_FILE}
fi

echo 
echo "*** Removing the old image folders: ${PACKR_TARGET_FOLDER} ***"
rm -fr ${PACKR_TARGET_FOLDER} || true

echo 
echo "*** Building '${PACKAGE_NAME}' image using packr ***"
time java -jar ../packr.jar \
     --platform linux64 \
     --executable ${EXE_NAME} \
     --classpath ${RECORD_AND_UPLOAD_JAR} \
     --jdk ${JRE_ZIP_FILE_NAME} \
     --mainclass tdl.record_upload.RecordAndUploadApp \
     --vmargs Xmx2G \
     --output ${PACKR_TARGET_FOLDER}

HUMBLE_LINUX_LIB=libhumblevideo.so
ZIP_ARCHIVE_NAME=$(getZipArchiveName "${RELEASE_VERSION}-linux")
echo "*** Uncompressing  ${HUMBLE_LINUX_LIB} from ${RECORD_AND_UPLOAD_JAR} into '${PACKR_TARGET_FOLDER}' ***"
time unzip -o ${PACKR_TARGET_FOLDER}/${RECORD_AND_UPLOAD_JAR} ${HUMBLE_LINUX_LIB}
mv ${HUMBLE_LINUX_LIB} ${PACKR_TARGET_FOLDER}

echo "*** Removing ${HUMBLE_LINUX_LIB} from ${RECORD_AND_UPLOAD_JAR} in '${PACKR_TARGET_FOLDER}' ***"
time zip -d ${PACKR_TARGET_FOLDER}/${RECORD_AND_UPLOAD_JAR} ${HUMBLE_LINUX_LIB}

echo "*** Compressing '${PACKR_TARGET_FOLDER}' into '${ZIP_ARCHIVE_NAME}' ***"
time zip -r ${ZIP_ARCHIVE_NAME} ${PACKR_TARGET_FOLDER}

# Enable for debug purposes
#echo "./record-and-upload --config /config/credentials.config --store /localstore/" > record/runJar.sh
#chmod +x record/runJar.sh
