package org.thingsboard.server.dao;

import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.junit.ClassRule;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.extensions.cpsuite.ClasspathSuite.ClassnameFilters;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(ClasspathSuite.class)
@ClassnameFilters({
        "org.thingsboard.server.dao.service.*.nosql.*ServiceNoSqlTest",
})
public class NoSqlDaoServiceTestSuite {

    @ClassRule
    public static CustomCassandraCQLUnit cassandraUnit =
            new CustomCassandraCQLUnit(
                    Arrays.asList(
                            new ClassPathCQLDataSet("cassandra/schema-keyspace.cql", false, false),
                            new ClassPathCQLDataSet("cassandra/schema-ts.cql", false, false),
                            new ClassPathCQLDataSet("cassandra/schema-ts-latest.cql", false, false)
                    ),
                    "cassandra-test.yaml", 30000L);

}
