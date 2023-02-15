package org.thingsboard.server.dao.cassandra.guava;

import com.datastax.oss.driver.api.core.session.Session;
import com.datastax.oss.driver.internal.core.session.SessionWrapper;

public class DefaultGuavaSession extends SessionWrapper implements GuavaSession {

    public DefaultGuavaSession(Session delegate) {
        super(delegate);
    }
}
