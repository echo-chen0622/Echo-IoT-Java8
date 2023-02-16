#!/bin/bash

echo "Starting '${project.name}' ..."

CONF_FOLDER="${pkg.installFolder}/conf"

configfile=${pkg.name}.conf

source "${CONF_FOLDER}/${configfile}"

cd ${pkg.installFolder}

# This will forward this PID 1 to the node.js and forward SIGTERM for graceful shutdown as well
exec node server.js
