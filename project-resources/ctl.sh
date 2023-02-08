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

start_arango () {
  echo "Starting ArangoDB"
  ${OCI_RUNTIME} run -d \
   --name data_arangodb \
   --volume ${ARANGO_DATA_DIR}:/var/lib/arangodb3:z \
   --volume ${ARANGO_APPS_DIR}:/var/lib/arangodb3-apps:z \
   --env ARANGO_ROOT_PASSWORD=${ARANGO_ROOT_PASSWORD} \
   --publish ${ARANGO_PORT}:${ARANGO_PORT} \
   docker.io/arangodb:3.10
}

start_app () {
  echo "Starting the app"
  pushd ../ > /dev/null
  gradle clean bootRun > "${RUN_DIR}/application.log" 2>&1 &
  app_pid="$!"
  echo "${app_pid}" > "${RUN_DIR}/.pid"
  echo "Application started with pid: ${app_pid}"
  popd > /dev/null
  echo "To see the application logs, tail -f ${RUN_DIR}/application.log"
}

start () {
  start_arango
  start_app
}

stop () {
  stop_app
  stop_arango
}

stop_app () {
  echo "Stopping the app"
  pkill -F "${RUN_DIR}/.pid" && rm "${RUN_DIR}/.pid"
}

stop_arango () {
  echo "Stopping ArangoDB"
  ${OCI_RUNTIME} stop data_arangodb
  ${OCI_RUNTIME} rm data_arangodb
}

init () {
  echo "Initializing"
  # Ensure that ArangoDB directories exist
  source ./.env
  mkdir -p "${ARANGO_APPS_DIR}"
  mkdir -p "${ARANGO_DATA_DIR}"
  OCI_RUNTIME=$(get_oci_runtime)
  echo "OCI runtime: ${OCI_RUNTIME}"
  ${OCI_RUNTIME} image ls | grep -q docker.io/arangodb:3.10 || podman pull docker.io/arangodb:3.10
}

usage () {
	echo "Starts/stops the File Locator application and ArangoDB."
	echo -e "Usage: $0 [option]"
	echo -e "Options:"
	echo -e "  start:        Starts the app and ArangoDB"
	echo -e "  stop:         Stops the app and ArangoDB"
	echo -e "  startapp:     Starts the app"
	echo -e "  startdb:      Starts ArangoDB"
	echo -e "  stopapp:      Stops the app"
	echo -e "  stopdb:       Staops ArangoDB"
}

main () {
  start_dir=$(pwd)
  pushd "$(dirname "$0")" > /dev/null || echo "Could not switch to program dir"
  case "${1}" in
    'start')
      action='start'
      ;;
    'startapp')
      action='start_app'
      ;;
    'startdb')
      action='start_arango'
      ;;
    'stop')
      action='stop'
      ;;
    'stop_app')
      action='stop_app'
      ;;
    'stop_db')
      action='stop_arango'
      ;;
    *)
      echo "Unrecognized option: ${1}"
      usage
      exit
      ;;
  esac
  init
  eval ${action}
  popd > /dev/null || echo "Could not return to original dir: ${start_dir}"
}

main "$@"