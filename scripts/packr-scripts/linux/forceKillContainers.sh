#!/bin/bash

set -e
set -u
set -o pipefail

echo "Terminating all running containers"
docker ps -a
docker rm -f $(docker ps -a -q)
echo "All running containers terminated"