package org.echoiot.server.dao.sql.relation;

import org.echoiot.common.util.AbstractListeningExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JpaRelationQueryExecutorService extends AbstractListeningExecutor {

    @Value("${sql.relations.pool_size:4}")
    private int poolSize;

    @Override
    protected int getThreadPollSize() {
        return poolSize;
    }

}
