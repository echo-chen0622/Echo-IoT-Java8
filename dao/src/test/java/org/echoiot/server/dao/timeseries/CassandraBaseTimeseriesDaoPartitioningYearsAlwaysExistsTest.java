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
import java.util.List;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CassandraBaseTimeseriesDao.class)
@TestPropertySource(properties = {
        "database.ts.type=cassandra",
        "cassandra.query.ts_key_value_partitioning=YEARS",
        "cassandra.query.use_ts_key_value_partitioning_on_read=false",
        "cassandra.query.ts_key_value_partitions_max_cache_size=100000",
        "cassandra.query.ts_key_value_partitions_cache_stats_enabled=true",
        "cassandra.query.ts_key_value_partitions_cache_stats_interval=60",
        "cassandra.query.ts_key_value_ttl=0",
        "cassandra.query.set_null_values_enabled=false",
})
@Slf4j
public class CassandraBaseTimeseriesDaoPartitioningYearsAlwaysExistsTest {

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
    public void testToPartitionsYears() throws ParseException {
        assertThat(tsDao.getPartitioning()).isEqualTo("YEARS");
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:01Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T23:59:59Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T23:59:59Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-01-01T00:00:00Z").getTime());
    }

    @Test
    public void testCalculatePartitionsYears() throws ParseException {
        long startTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-01-01T00:00:00Z").getTime());
        long nextTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2021-10-12T23:59:59Z").getTime());
        long endTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2025-07-15T00:00:00Z").getTime());
        log.info("startTs {}, nextTs {}, endTs {}", startTs, nextTs, endTs);

        assertThat(tsDao.calculatePartitions(0, 0)).isEqualTo(List.of(0L));
        assertThat(tsDao.calculatePartitions(0, 1)).isEqualTo(List.of(0L, 1L));

        assertThat(tsDao.calculatePartitions(startTs, startTs)).isEqualTo(List.of(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-01-01T00:00:00Z").getTime()));
        assertThat(tsDao.calculatePartitions(startTs, nextTs)).isEqualTo(List.of(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-01T00:00:00Z").getTime()));

        assertThat(tsDao.calculatePartitions(startTs, endTs)).hasSize(7).isEqualTo(List.of(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2024-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2025-01-01T00:00:00Z").getTime()));
    }

}
