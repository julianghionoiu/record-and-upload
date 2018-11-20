#!/bin/bash

source ../linux-common-env-variables.sh

JRE_ZIP_FILE_NAME=$(ls jre*.zip) #jre1.8.0_152.zip

if [[ -z ${JRE_ZIP_FILE_NAME} ]]; then
   echo "JRE for Linux was not found, please place one of them in the current directory and try running the script again."
   echo "Process halted."
else 
   echo "${JRE_ZIP_FILE_NAME}: JRE for Linux found."
fi

echo 
echo "*** Removing the old image folders: ${PACKR_TARGET_FOLDER} ***"
rm -fr ${PACKR_TARGET_FOLDER} || true

echo 
echo "*** Building ${PACKAGE_NAME} image using packr ***"
time java -jar ../packr.jar \
     --platform linux64 \
     --executable ${EXE_NAME} \
     --classpath ${RECORD_AND_UPLOAD_JAR} \
     --jdk ${JRE_ZIP_FILE_NAME} \
     --mainclass tdl.record_upload.RecordAndUploadApp \
     --vmargs Xmx2G \
     --minimizejre ../reduced-jre.json \
     --output ${PACKR_TARGET_FOLDER}

time cd ${PACKR_TARGET_FOLDER} && zip -r ../${ZIP_ARCHIVE_NAME} .