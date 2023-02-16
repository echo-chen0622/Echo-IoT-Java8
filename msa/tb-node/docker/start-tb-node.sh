#!/bin/bash
CONF_FOLDER="/config"
jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf
run_user=${pkg.user}

source "${CONF_FOLDER}/${configfile}"

export LOADER_PATH=/config,${LOADER_PATH}

cd ${pkg.installFolder}/bin

if [ "$INSTALL_TB" == "true" ]; then

    if [ "$LOAD_DEMO" == "true" ]; then
        loadDemo=true
    else
        loadDemo=false
    fi

    echo "Starting Echoiot installation ..."

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.echoiot.server.EchoiotInstallApplication \
                        -Dinstall.load_demo=${loadDemo} \
                        -Dspring.jpa.hibernate.ddl-auto=none \
                        -Dinstall.upgrade=false \
                        -Dlogging.config=/usr/share/echoiot/bin/install/logback.xml \
                        org.springframework.boot.loader.PropertiesLauncher

elif [ "$UPGRADE_TB" == "true" ]; then

    echo "Starting Echoiot upgrade ..."

    if [[ -z "${FROM_VERSION// }" ]]; then
        echo "FROM_VERSION variable is invalid or unspecified!"
        exit 1
    else
        fromVersion="${FROM_VERSION// }"
    fi

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.echoiot.server.EchoiotInstallApplication \
                    -Dspring.jpa.hibernate.ddl-auto=none \
                    -Dinstall.upgrade=true \
                    -Dinstall.upgrade.from_version=${fromVersion} \
                    -Dlogging.config=/usr/share/echoiot/bin/install/logback.xml \
                    org.springframework.boot.loader.PropertiesLauncher

else

    echo "Starting '${project.name}' ..."

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.echoiot.server.EchoiotServerApplication \
                        -Dspring.jpa.hibernate.ddl-auto=none \
                        -Dlogging.config=/config/logback.xml \
                        org.springframework.boot.loader.PropertiesLauncher

fi
