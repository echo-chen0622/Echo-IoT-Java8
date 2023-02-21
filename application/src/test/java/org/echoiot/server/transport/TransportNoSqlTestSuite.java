package org.echoiot.server.transport;

import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.echoiot.server.dao.CustomCassandraCQLUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.ClassRule;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({
        "org.echoiot.server.transport.*.telemetry.timeseries.nosql.*Test",
})
public class TransportNoSqlTestSuite {

    @NotNull
    @ClassRule
    public static CustomCassandraCQLUnit cassandraUnit =
            new CustomCassandraCQLUnit(
                    Arrays.asList(
                            new ClassPathCQLDataSet("cassandra/schema-keyspace.cql", false, false),
                            new ClassPathCQLDataSet("cassandra/schema-ts.cql", false, false),
                            new ClassPathCQLDataSet("cassandra/schema-ts-latest.cql", false, false)
                    ),
                    "cassandra-test.yaml", 30000L
            );

}
