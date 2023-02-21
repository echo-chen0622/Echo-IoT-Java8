package org.echoiot.server.dao.nosql;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.cassandra.CassandraCluster;
import org.echoiot.server.dao.cassandra.guava.GuavaSession;
import org.echoiot.server.dao.timeseries.CassandraBaseTimeseriesDao;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CassandraPartitionsCacheTest {

    @Spy
    private CassandraBaseTimeseriesDao cassandraBaseTimeseriesDao;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private BoundStatement boundStatement;

    @Mock
    private Environment environment;

    @Mock
    private CassandraCluster cluster;

    @Mock
    private GuavaSession session;

    @Before
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "partitioning", "MONTHS");
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "partitionsCacheSize", 100000);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "systemTtl", 0);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "setNullValuesEnabled", false);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "environment", environment);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "cluster", cluster);

        when(cluster.getDefaultReadConsistencyLevel()).thenReturn(ConsistencyLevel.ONE);
        when(cluster.getDefaultWriteConsistencyLevel()).thenReturn(ConsistencyLevel.ONE);
        when(cluster.getSession()).thenReturn(session);
        when(session.prepare(anyString())).thenReturn(preparedStatement);

        when(preparedStatement.bind()).thenReturn(boundStatement);

        when(boundStatement.setString(anyInt(), anyString())).thenReturn(boundStatement);
        when(boundStatement.setUuid(anyInt(), any(UUID.class))).thenReturn(boundStatement);
        when(boundStatement.setLong(anyInt(), any(Long.class))).thenReturn(boundStatement);

        willReturn(new TbResultSetFuture(SettableFuture.create())).given(cassandraBaseTimeseriesDao).executeAsyncWrite(any(), any());

        doReturn(Futures.immediateFuture(0)).when(cassandraBaseTimeseriesDao).getFuture(any(), any());
    }

    @Test
    public void testPartitionSave() throws Exception {
        cassandraBaseTimeseriesDao.init();

        @NotNull UUID id = UUID.randomUUID();
        TenantId tenantId = TenantId.fromUUID(id);
        long tsKvEntryTs = System.currentTimeMillis();

        for (int i = 0; i < 50000; i++) {
            cassandraBaseTimeseriesDao.savePartition(tenantId, tenantId, tsKvEntryTs, "test" + i);
        }
        for (int i = 0; i < 60000; i++) {
            cassandraBaseTimeseriesDao.savePartition(tenantId, tenantId, tsKvEntryTs, "test" + i);
        }
        verify(cassandraBaseTimeseriesDao, times(60000)).executeAsyncWrite(any(TenantId.class), any(Statement.class));
    }

}
