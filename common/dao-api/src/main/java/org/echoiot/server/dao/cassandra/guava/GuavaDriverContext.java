package org.echoiot.server.dao.cassandra.guava;

import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.PrepareRequest;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.api.core.session.ProgrammaticArguments;
import com.datastax.oss.driver.internal.core.context.DefaultDriverContext;
import com.datastax.oss.driver.internal.core.cql.CqlPrepareAsyncProcessor;
import com.datastax.oss.driver.internal.core.cql.CqlPrepareSyncProcessor;
import com.datastax.oss.driver.internal.core.cql.CqlRequestAsyncProcessor;
import com.datastax.oss.driver.internal.core.cql.CqlRequestSyncProcessor;
import com.datastax.oss.driver.internal.core.session.RequestProcessorRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * A Custom {@link DefaultDriverContext} that overrides {@link #getRequestProcessorRegistry()} to
 * return a {@link RequestProcessorRegistry} that includes processors for returning guava futures.
 */
public class GuavaDriverContext extends DefaultDriverContext {

    public GuavaDriverContext(@NotNull DriverConfigLoader configLoader, @NotNull ProgrammaticArguments programmaticArguments) {
        super(configLoader, programmaticArguments);
    }

    @NotNull
    @Override
    public RequestProcessorRegistry buildRequestProcessorRegistry() {
        // Register the typical request processors, except instead of the normal async processors,
        // use GuavaRequestAsyncProcessor to return ListenableFutures in async methods.

        @NotNull CqlRequestAsyncProcessor cqlRequestAsyncProcessor = new CqlRequestAsyncProcessor();
        @NotNull CqlPrepareAsyncProcessor cqlPrepareAsyncProcessor = new CqlPrepareAsyncProcessor();
        @NotNull CqlRequestSyncProcessor cqlRequestSyncProcessor =
                new CqlRequestSyncProcessor(cqlRequestAsyncProcessor);

        return new RequestProcessorRegistry(
                getSessionName(),
                cqlRequestSyncProcessor,
                new CqlPrepareSyncProcessor(cqlPrepareAsyncProcessor),
                new GuavaRequestAsyncProcessor<>(
                        cqlRequestAsyncProcessor, Statement.class, GuavaSession.ASYNC),
                new GuavaRequestAsyncProcessor<>(
                        cqlPrepareAsyncProcessor, PrepareRequest.class, GuavaSession.ASYNC_PREPARED));
    }
}
