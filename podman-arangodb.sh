#!/bin/bash

source ./.env

mkdir -p ${ARANGO_APPS_DIR}
mkdir -p ${ARANGO_DATA_DIR}

podman run -d \
 --name data_arangodb \
 --volume ${ARANGO_DATA_DIR}:/var/lib/arangodb3:z \
 --volume ${ARANGO_APPS_DIR}:/var/lib/arangodb3-apps:z \
 --env ARANGO_ROOT_PASSWORD=${ARANGO_ROOT_PASSWORD} \
 --publish ${ARANGO_PORT}:${ARANGO_PORT} \
 docker.io/arangodb:3.10
