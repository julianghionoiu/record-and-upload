#!/bin/bash

source ../linux-common-env-variables.sh

echo 
echo "*** Removing the old image folders: ${PACKR_TARGET_FOLDER} ***"
rm -fr ${PACKR_TARGET_FOLDER} || true

echo 
echo "*** Building ${PACKAGE_NAME} image using packr ***"
time java -jar ../packr.jar \
     --platform linux64 \
     --executable ${EXE_NAME} \
     --classpath ${RECORD_AND_UPLOAD_JAR} \
     --jdk jre1.8.0_152.zip \
     --mainclass tdl.record_upload.RecordAndUploadApp \
     --vmargs Xmx2G \
     --minimizejre ../reduced-jre.json \
     --output ${PACKR_TARGET_FOLDER}

time cd ${PACKR_TARGET_FOLDER} && zip -r ../${ZIP_ARCHIVE_NAME} .