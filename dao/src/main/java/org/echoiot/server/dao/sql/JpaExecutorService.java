package org.echoiot.server.dao.sql;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.echoiot.common.util.AbstractListeningExecutor;

@Component
public class JpaExecutorService extends AbstractListeningExecutor {

    @Value("${spring.datasource.hikari.maximumPoolSize}")
    private int poolSize;

    @Override
    protected int getThreadPollSize() {
        return poolSize;
    }

}
