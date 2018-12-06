#!/bin/bash

set -e
set -u
set -o pipefail

docker images -f dangling=true -q | xargs -I {} docker rmi {}

mkdir -p localstore

docker run -it \
           --rm  \
           --volume ${PWD}/record:/record-linux-packr \
           --volume ${HOME}/path/to/config:/config \
           --volume localstore:/localstore \
           --workdir /record-linux-packr \
           ubuntu:18.04 /bin/bash
