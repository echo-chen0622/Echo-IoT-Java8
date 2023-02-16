#!/bin/bash
set -e

source compose-utils.sh

COMPOSE_VERSION=$(composeVersion) || exit $?

ADDITIONAL_COMPOSE_QUEUE_ARGS=$(additionalComposeQueueArgs) || exit $?

ADDITIONAL_COMPOSE_ARGS=$(additionalComposeArgs) || exit $?

ADDITIONAL_CACHE_ARGS=$(additionalComposeCacheArgs) || exit $?

COMPOSE_ARGS_PULL="\
      -f docker-compose.yml ${ADDITIONAL_CACHE_ARGS} ${ADDITIONAL_COMPOSE_ARGS} ${ADDITIONAL_COMPOSE_QUEUE_ARGS} \
      pull"

COMPOSE_ARGS_BUILD="\
      -f docker-compose.yml ${ADDITIONAL_CACHE_ARGS} ${ADDITIONAL_COMPOSE_ARGS} ${ADDITIONAL_COMPOSE_QUEUE_ARGS} \
      up -d --no-deps --build"

case $COMPOSE_VERSION in
    V2)
        docker compose $COMPOSE_ARGS_PULL $@
        docker compose $COMPOSE_ARGS_BUILD $@
    ;;
    V1)
        docker-compose $COMPOSE_ARGS_PULL $@
        docker-compose $COMPOSE_ARGS_BUILD $@
    ;;
    *)
        # unknown option
    ;;
esac
