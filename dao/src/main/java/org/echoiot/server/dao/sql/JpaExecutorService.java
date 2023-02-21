package org.echoiot.server.dao.sql;

import org.echoiot.common.util.AbstractListeningExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JpaExecutorService extends AbstractListeningExecutor {

    @Value("${spring.datasource.hikari.maximumPoolSize}")
    private int poolSize;

    @Override
    protected int getThreadPollSize() {
        return poolSize;
    }

}
