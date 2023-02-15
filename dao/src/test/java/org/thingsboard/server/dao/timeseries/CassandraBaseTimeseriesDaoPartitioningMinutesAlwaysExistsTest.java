package org.thingsboard.server.dao.timeseries;

import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateReadExecutor;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateWriteExecutor;

import java.text.ParseException;
import java.util.List;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CassandraBaseTimeseriesDao.class)
@TestPropertySource(properties = {
        "database.ts.type=cassandra",
        "cassandra.query.ts_key_value_partitioning=MINUTES",
        "cassandra.query.use_ts_key_value_partitioning_on_read=false",
        "cassandra.query.ts_key_value_partitions_max_cache_size=100000",
        "cassandra.query.ts_key_value_partitions_cache_stats_enabled=true",
        "cassandra.query.ts_key_value_partitions_cache_stats_interval=60",
        "cassandra.query.ts_key_value_ttl=0",
        "cassandra.query.set_null_values_enabled=false",
})
@Slf4j
public class CassandraBaseTimeseriesDaoPartitioningMinutesAlwaysExistsTest {

    @Autowired
    CassandraBaseTimeseriesDao tsDao;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    @Qualifier("CassandraCluster")
    CassandraCluster cassandraCluster;

    @MockBean
    CassandraBufferedRateReadExecutor cassandraBufferedRateReadExecutor;
    @MockBean
    CassandraBufferedRateWriteExecutor cassandraBufferedRateWriteExecutor;

    @Test
    public void testToPartitionsMinutes() throws ParseException {
        assertThat(tsDao.getPartitioning()).isEqualTo("MINUTES");
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-02T00:01:00Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-02T00:01:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-03T00:02:01Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-03T00:02:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T23:59:59Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T23:59:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T23:59:59Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T23:59:00Z").getTime());
    }


    @Test
    public void testCalculatePartitionsMinutes() throws ParseException {
        long startTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime());
        long nextTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:02:59Z").getTime());
        long endTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:10:00Z").getTime());
        log.info("startTs {}, nextTs {}, endTs {}", startTs, nextTs, endTs);

        assertThat(tsDao.calculatePartitions(0, 0)).isEqualTo(List.of(0L));
        assertThat(tsDao.calculatePartitions(0, 1)).isEqualTo(List.of(0L, 1L));

        assertThat(tsDao.calculatePartitions(startTs, startTs)).isEqualTo(List.of(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime()));
        assertThat(tsDao.calculatePartitions(startTs, nextTs)).isEqualTo(List.of(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:01:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:02:00Z").getTime()));

        assertThat(tsDao.calculatePartitions(startTs, endTs)).hasSize(11).isEqualTo(List.of(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:01:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:02:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:03:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:04:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:05:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:06:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:07:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:08:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:09:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-10-10T00:10:00Z").getTime()));
    }

}
