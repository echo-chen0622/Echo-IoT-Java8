FROM echoiot/openjdk11:bullseye-slim

COPY start-tb-vc-executor.sh ${pkg.name}.deb /tmp/

RUN mkdir -p /home/echoiot/.config/jgit \
    && chown -R ${pkg.user}:${pkg.user} /home/echoiot \
    && chmod a+x /tmp/*.sh \
    && mv /tmp/start-tb-vc-executor.sh /usr/bin && \
    (yes | dpkg -i /tmp/${pkg.name}.deb) && \
    rm /tmp/${pkg.name}.deb && \
    (systemctl --no-reload disable --now ${pkg.name}.service > /dev/null 2>&1 || :) && \
    chmod 555 ${pkg.installFolder}/bin/${pkg.name}.jar

USER ${pkg.user}

CMD ["start-tb-vc-executor.sh"]
