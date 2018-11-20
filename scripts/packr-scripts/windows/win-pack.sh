#!/bin/bash

source ../linux-common-env-variables.sh

echo 
echo "*** Removing the old image folder: ${PACKR_TARGET_FOLDER} ***"
rm -fr ${PACKR_TARGET_FOLDER} || true

echo 
echo "*** Removing the old ${ZIP_ARCHIVE_NAME} archive ***"
rm -fr ${ZIP_ARCHIVE_NAME} || true

time java -jar ../packr.jar \
     --platform windows64 \
     --executable ${EXE_NAME} \
     --classpath ${RECORD_AND_UPLOAD_JAR} \
     --jdk jdk1.8.0_111.zip \
     --mainclass tdl.record_upload.RecordAndUploadApp \
     --vmargs Xmx2G \
     --minimizejre ../reduced-jre.json \
     --output ${PACKR_TARGET_FOLDER}

time cd ${PACKR_TARGET_FOLDER} && zip -r ../${ZIP_ARCHIVE_NAME} .