#!/bin/bash

set -e
set -u
set -o pipefail

docker images -f dangling=true -q | xargs -I {} docker rmi {}

mkdir -p localstore

echo "Run the below inside the container:"
echo ""
echo "    ./record/record-and-upload --config \$(pwd)/config/credentials.config --store \$(pwd)/localstore"
echo " or"
echo "    ./record/jre/bin/java -jar \$(pwd)/record/record-and-upload-0.0.16-linux.jar --config \$(pwd)/config/credentials.config --store \$(pwd)/localstore"
echo ""

docker run -it \
           --rm  \
           --volume ${PWD}:/record-linux-packr    \
           --volume ${PWD}/localstore:/localstore \
           --volume ${PWD}/config:/config         \
           --workdir /record-linux-packr          \
           ubuntu:18.04 /bin/bash
