package org.echoiot.server.dao.cassandra.guava;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.context.DriverContext;
import com.datastax.oss.driver.api.core.session.ProgrammaticArguments;
import com.datastax.oss.driver.api.core.session.SessionBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.jetbrains.annotations.NotNull;

public class GuavaSessionBuilder extends SessionBuilder<GuavaSessionBuilder, GuavaSession> {

    @NotNull
    @Override
    protected DriverContext buildContext(@NotNull DriverConfigLoader configLoader, @NotNull ProgrammaticArguments programmaticArguments) {
        return new GuavaDriverContext(configLoader, programmaticArguments);
    }

    @NotNull
    @Override
    protected GuavaSession wrap(@NonNull CqlSession defaultSession) {
        return new DefaultGuavaSession(defaultSession);
    }
}
