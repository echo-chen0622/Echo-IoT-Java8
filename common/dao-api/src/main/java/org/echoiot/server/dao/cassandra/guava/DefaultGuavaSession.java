package org.echoiot.server.dao.cassandra.guava;

import com.datastax.oss.driver.api.core.session.Session;
import com.datastax.oss.driver.internal.core.session.SessionWrapper;
import org.jetbrains.annotations.NotNull;

public class DefaultGuavaSession extends SessionWrapper implements GuavaSession {

    public DefaultGuavaSession(@NotNull Session delegate) {
        super(delegate);
    }
}
