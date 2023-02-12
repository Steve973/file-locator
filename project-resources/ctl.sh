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
	echo -e 'Options:'
	echo -e '  --start:         Starts the app and ArangoDB'
	echo -e '  --stop:          Stops the app and ArangoDB'
	echo -e '  --start-app:     Starts the app'
	echo -e '  --start-db:      Starts ArangoDB'
	echo -e '  --stop-app:      Stops the app'
	echo -e '  --stop-db:       Staops ArangoDB'
}

process_action () {
  TEMP=$(getopt -o hspadxy --long help,start,stop,start-app,start-db,stop-app,stop-db -- "$1")
  eval set -- "$TEMP"
  local action='--help'
  case "$1" in
    -s|--start)
      action='init && start'
      ;;
    -p|--stop)
      action='init && stop'
      ;;
    -a|--start-app)
      action='init && start_app'
      ;;
    -d|--start-db)
      action='init && start_arango'
      ;;
    -x|--stop-app)
      action='init && stop_app'
      ;;
    -y|--stop-db)
      action='init && stop_arango'
      ;;
    -h|--help)
      ;&
    *)
      action='usage && exit 1'
      ;;
  esac
  echo "${action}"
}

main () {
  start_dir=$(pwd)
  pushd "$(dirname "$0")" > /dev/null || echo "Could not switch to program dir"
  action=$(process_action "$1")
  eval "${action}"
  popd > /dev/null || echo "Could not return to original dir: ${start_dir}"
}

main "$@"