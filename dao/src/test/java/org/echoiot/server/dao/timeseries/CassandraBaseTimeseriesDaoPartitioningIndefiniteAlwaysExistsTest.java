package org.echoiot.server.dao.timeseries;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.cassandra.CassandraCluster;
import org.echoiot.server.dao.nosql.CassandraBufferedRateReadExecutor;
import org.echoiot.server.dao.nosql.CassandraBufferedRateWriteExecutor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.text.ParseException;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CassandraBaseTimeseriesDao.class)
@TestPropertySource(properties = {
        "database.ts.type=cassandra",
        "cassandra.query.ts_key_value_partitioning=INDEFINITE",
        "cassandra.query.use_ts_key_value_partitioning_on_read=false",
        "cassandra.query.ts_key_value_partitions_max_cache_size=100000",
        "cassandra.query.ts_key_value_partitions_cache_stats_enabled=true",
        "cassandra.query.ts_key_value_partitions_cache_stats_interval=60",
        "cassandra.query.ts_key_value_ttl=0",
        "cassandra.query.set_null_values_enabled=false",
})
@Slf4j
public class CassandraBaseTimeseriesDaoPartitioningIndefiniteAlwaysExistsTest {

    @Resource
    CassandraBaseTimeseriesDao tsDao;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    @Qualifier("CassandraCluster")
    CassandraCluster cassandraCluster;

    @MockBean
    CassandraBufferedRateReadExecutor cassandraBufferedRateReadExecutor;
    @MockBean
    CassandraBufferedRateWriteExecutor cassandraBufferedRateWriteExecutor;

    @Test
    public void testToPartitionsIndefinite() throws ParseException {
        assertThat(tsDao.getPartitioning()).isEqualTo("INDEFINITE");
        assertThat(tsDao.toPartitionTs(ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime())).isEqualTo(0L);
    }


    @Test
    public void testCalculatePartitionsIndefinite() throws ParseException {
       //Indefinite partitioning should never call tsDao.calculatePartitions()
    }

}
