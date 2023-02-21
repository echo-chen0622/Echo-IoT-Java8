package org.echoiot.server.dao.sqlts;

import com.google.common.util.concurrent.Futures;
import org.assertj.core.api.Assertions;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.echoiot.server.common.data.kv.BaseReadTsKvQuery;
import org.echoiot.server.common.data.kv.ReadTsKvQuery;
import org.echoiot.server.common.data.kv.ReadTsKvQueryResult;
import org.echoiot.server.common.data.kv.TsKvEntry;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AbstractChunkedAggregationTimeseriesDaoTest {

    final int LIMIT = 1;
    final String TEMP = "temp";
    final String DESC = "DESC";
    private AbstractChunkedAggregationTimeseriesDao tsDao;

    @Before
    public void setUp() throws Exception {
        tsDao = spy(AbstractChunkedAggregationTimeseriesDao.class);
        @NotNull Optional<TsKvEntry> optionalListenableFuture = Optional.of(mock(TsKvEntry.class));
        willReturn(Futures.immediateFuture(optionalListenableFuture)).given(tsDao).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        willReturn(Futures.immediateFuture(mock(ReadTsKvQueryResult.class))).given(tsDao).getReadTsKvQueryResultFuture(any(), any());
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenLastIntervalShorterThanOthersAndEqualsEndTs() {
        @NotNull ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 2000, LIMIT, Aggregation.COUNT, DESC);
        @NotNull ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 2001, 1001, LIMIT, Aggregation.COUNT, DESC);
        @NotNull ReadTsKvQuery subQuerySecond = new BaseReadTsKvQuery(TEMP, 2001, 3000, 2501, LIMIT, Aggregation.COUNT, DESC);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(2)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, subQueryFirst.getKey(), 1, 2001, getTsForReadTsKvQuery(1, 2001), Aggregation.COUNT);
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, subQuerySecond.getKey(), 2001, 3000, getTsForReadTsKvQuery(2001, 3000), Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriod() {
        @NotNull ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 3000, LIMIT, Aggregation.COUNT, DESC);
        @NotNull ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3001, 1501, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        Assertions.assertThat(tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query)).isNotNull();
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriodMinusOne() {
        @NotNull ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 2999, LIMIT, Aggregation.COUNT, DESC);
        @NotNull ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3000, 1500, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), Aggregation.COUNT);

    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriodPlusOne() {
        @NotNull ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 3001, LIMIT, Aggregation.COUNT, DESC);
        @NotNull ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3001, 1501, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsZero() {
        @NotNull ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 0, 0, 1, LIMIT, Aggregation.COUNT, DESC);
        @NotNull ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 0, 1, 0, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, subQueryFirst.getKey(), 0, 1, getTsForReadTsKvQuery(0, 1), Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsOne() {
        @NotNull ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 1, 1, LIMIT, Aggregation.COUNT, DESC);
        @NotNull ReadTsKvQuery subQuery = new BaseReadTsKvQuery(TEMP, 1, 2, 1, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, subQuery.getKey(), 1, 2, getTsForReadTsKvQuery(1, 2), Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsIntegerMax() {
        @NotNull ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, Integer.MAX_VALUE, Integer.MAX_VALUE, 1, LIMIT, Aggregation.COUNT, DESC);
        @NotNull ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L, Integer.MAX_VALUE, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, subQueryFirst.getKey(), Integer.MAX_VALUE, 1L + Integer.MAX_VALUE, getTsForReadTsKvQuery(Integer.MAX_VALUE, 1L + Integer.MAX_VALUE), Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsBigNumber() {
        @NotNull ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, Integer.MAX_VALUE, LIMIT, Aggregation.COUNT, DESC);
        @NotNull ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3001, 1501, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), Aggregation.COUNT);
    }

    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenCountIntervalEqualsPeriodSize() {
        @NotNull ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 3, LIMIT, Aggregation.COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        tsDao.findAllAsync(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, query);
        verify(tsDao, times(1000)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        for (long i = 1; i <= 3000; i += 3) {
            verify(tsDao, times(1)).findAndAggregateAsync(TenantId.SYS_TENANT_ID, TEMP, i, Math.min(i + 3, 3000), getTsForReadTsKvQuery(i, i + 3), Aggregation.COUNT);
        }
    }

    long getTsForReadTsKvQuery(long startTs, long endTs) {
        return startTs + (endTs - startTs) / 2L;
    }

}
