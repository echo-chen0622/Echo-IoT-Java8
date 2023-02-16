#!/bin/bash
set -e

source compose-utils.sh

COMPOSE_VERSION=$(composeVersion) || exit $?

ADDITIONAL_COMPOSE_QUEUE_ARGS=$(additionalComposeQueueArgs) || exit $?

ADDITIONAL_COMPOSE_ARGS=$(additionalComposeArgs) || exit $?

ADDITIONAL_CACHE_ARGS=$(additionalComposeCacheArgs) || exit $?

ADDITIONAL_COMPOSE_MONITORING_ARGS=$(additionalComposeMonitoringArgs) || exit $?

checkFolders --create || exit $?

COMPOSE_ARGS="\
      -f docker-compose.yml ${ADDITIONAL_CACHE_ARGS} ${ADDITIONAL_COMPOSE_ARGS} ${ADDITIONAL_COMPOSE_QUEUE_ARGS} ${ADDITIONAL_COMPOSE_MONITORING_ARGS} \
      up -d"

case $COMPOSE_VERSION in
    V2)
        docker compose $COMPOSE_ARGS
    ;;
    V1)
        docker-compose --compatibility $COMPOSE_ARGS
    ;;
    *)
        # unknown option
    ;;
esac
