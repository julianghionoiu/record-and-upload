#!/bin/bash

set -e
set -u
set -o pipefail

SCRIPT_DIR=$(realpath $(dirname $0))
PARENT_DIR=$(realpath ${SCRIPT_DIR}/..)
source ${PARENT_DIR}/common-env-variables.sh

cd ${SCRIPT_DIR}
JRE_ZIP_FILE_NAME=$(ls jre*.zip) #jre1.8.0_152.zip
RELEASE_VERSION=$(git describe --abbrev=0 --tags | tr -d v)
OS_NAME=linux
RECORD_AND_UPLOAD_JAR=${PACKAGE_NAME}-${OS_NAME}-${RELEASE_VERSION}.jar
TGZ_ARCHIVE_NAME=$(getArchiveName "${OS_NAME}-${RELEASE_VERSION}" "tgz")

# JRE_ZIP_ARCHIVE_LINUX_URL is an environment variable
download_os_specific_jre_archive ${JRE_ZIP_FILE_NAME} ${JRE_ZIP_ARCHIVE_LINUX_URL}

# This would be a wget command that downloads the jar from the record-and-upload git repo
# after the release process has push the artifact to github
# the artifact will contain the OS specific humble video and the uber jar for record-and-upload
if [[ ! -s ${RECORD_AND_UPLOAD_JAR} ]]; then
   echo "Jar file ${RECORD_AND_UPLOAD_JAR} not found"
   echo "Downloading ${RECORD_AND_UPLOAD_JAR} from github"
   wget https://github.com/julianghionoiu/record-and-upload/releases/download/${RELEASE_VERSION}/${RECORD_AND_UPLOAD_JAR} ${PACKAGE_NAME}
fi

echo 
echo "*** Removing the old image folder: ${PACKR_TARGET_FOLDER} ***"
rm -fr ${PACKR_TARGET_FOLDER} || true

echo
echo "*** Removing the old ${TGZ_ARCHIVE_NAME} archive ***"
rm -fr ${TGZ_ARCHIVE_NAME} || true

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
echo "*** Uncompressing  ${HUMBLE_LINUX_LIB} from ${RECORD_AND_UPLOAD_JAR} into '${PACKR_TARGET_FOLDER}' ***"
time unzip -o ${PACKR_TARGET_FOLDER}/${RECORD_AND_UPLOAD_JAR} ${HUMBLE_LINUX_LIB}
mv ${HUMBLE_LINUX_LIB} ${PACKR_TARGET_FOLDER}

echo "*** Making the commands in the bin directory of the JRE executable ***"
chmod +x ${PACKR_TARGET_FOLDER}/jre/bin/*

echo "*** Removing ${HUMBLE_LINUX_LIB} from ${RECORD_AND_UPLOAD_JAR} in '${PACKR_TARGET_FOLDER}' ***"
time zip -d ${PACKR_TARGET_FOLDER}/${RECORD_AND_UPLOAD_JAR} ${HUMBLE_LINUX_LIB}

echo "*** Compressing '${PACKR_TARGET_FOLDER}' into '${TGZ_ARCHIVE_NAME}' ***"
time tar -czvf ${TGZ_ARCHIVE_NAME} ${PACKR_TARGET_FOLDER}

cd -
# Enable for debug purposes
#echo "./record-and-upload --config /config/credentials.config --store /localstore/" > record/runJar.sh
#chmod +x record/runJar.sh
