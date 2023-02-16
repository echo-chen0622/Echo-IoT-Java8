FROM echoiot/openjdk11:bullseye-slim

COPY start-tb-node.sh ${pkg.name}.deb /tmp/

RUN echo 'networkaddress.cache.ttl=60' >> /etc/java-11-openjdk/security/java.security \
    && chmod a+x /tmp/*.sh \
    && mv /tmp/start-tb-node.sh /usr/bin && \
    (yes | dpkg -i /tmp/${pkg.name}.deb) && \
    rm /tmp/${pkg.name}.deb && \
    (systemctl --no-reload disable --now ${pkg.name}.service > /dev/null 2>&1 || :) && \
    chown -R ${pkg.user}:${pkg.user} /tmp && \
    chmod 555 ${pkg.installFolder}/bin/${pkg.name}.jar

USER ${pkg.user}

CMD ["start-tb-node.sh"]
