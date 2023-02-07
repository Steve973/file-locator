#!/bin/bash

get_oci_runtime () {
#  if [ -x "$(command -v podman)" ]; then
#    echo 'podman'
#  elif [ -x "$(command -v docker)" ]; then
#    echo 'docker'
#  else
#    echo 'Exiting: Podman or Docker required to run FileLocator, but neither were found'
#    exit 1
#  fi
echo 'docker'
}

start () {
  echo "Starting ArangoDB and app"

  ${OCI_RUNTIME} run -d \
   --name data_arangodb \
   --volume ${ARANGO_DATA_DIR}:/var/lib/arangodb3:z \
   --volume ${ARANGO_APPS_DIR}:/var/lib/arangodb3-apps:z \
   --env ARANGO_ROOT_PASSWORD=${ARANGO_ROOT_PASSWORD} \
   --publish ${ARANGO_PORT}:${ARANGO_PORT} \
   docker.io/arangodb:3.10

  pushd ../ > /dev/null
  gradle clean bootRun > "${RUN_DIR}/application.log" 2>&1 &
  app_pid="$!"
  echo "${app_pid}" > "${RUN_DIR}/.pid"
  echo "Application started with pid: ${app_pid}"
  popd > /dev/null
  echo "To see the application logs, tail -f ${RUN_DIR}/application.log"
}

stop () {
  echo "Stopping"
  pkill -F "${RUN_DIR}/.pid" && rm "${RUN_DIR}/.pid"
  ${OCI_RUNTIME} stop data_arangodb
  ${OCI_RUNTIME} rm data_arangodb
}

init () {
  # Ensure that ArangoDB directories exist
  mkdir -p "${ARANGO_APPS_DIR}"
  mkdir -p "${ARANGO_DATA_DIR}"

  ${OCI_RUNTIME} image ls | grep -q docker.io/arangodb:3.10 || podman pull docker.io/arangodb:3.10
}

main () {
  start_dir=$(pwd)
  pushd "$(dirname "$0")" > /dev/null || echo "Could not switch to program dir"

  source ./.env

  OCI_RUNTIME=$(get_oci_runtime)
  echo "OCI runtime: ${OCI_RUNTIME}"

  case "${1}" in
    'start')
      echo "Starting..."
      init
      start
      ;;
    'stop')
      echo "Stopping..."
      stop
      ;;
    *)
      echo "Unrecognized option: ${1}"
      exit
      ;;
  esac
  popd > /dev/null || echo "Could not return to original dir: ${start_dir}"
}

main "$@"