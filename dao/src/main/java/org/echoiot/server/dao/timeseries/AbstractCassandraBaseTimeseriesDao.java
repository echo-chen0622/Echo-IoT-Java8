package org.echoiot.server.dao.timeseries;

import com.datastax.oss.driver.api.core.cql.Row;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.nosql.CassandraAbstractAsyncDao;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class AbstractCassandraBaseTimeseriesDao extends CassandraAbstractAsyncDao {
    public static final String DESC_ORDER = "DESC";
    public static final String GENERATED_QUERY_FOR_ENTITY_TYPE_AND_ENTITY_ID = "Generated query [{}] for entityType {} and entityId {}";
    public static final String INSERT_INTO = "INSERT INTO ";
    public static final String SELECT_PREFIX = "SELECT ";
    public static final String EQUALS_PARAM = " = ? ";

    @Nullable
    public static KvEntry toKvEntry(Row row, String key) {
        @Nullable KvEntry kvEntry = null;
        @Nullable String strV = row.get(ModelConstants.STRING_VALUE_COLUMN, String.class);
        if (strV != null) {
            kvEntry = new StringDataEntry(key, strV);
        } else {
            @Nullable Long longV = row.get(ModelConstants.LONG_VALUE_COLUMN, Long.class);
            if (longV != null) {
                kvEntry = new LongDataEntry(key, longV);
            } else {
                @Nullable Double doubleV = row.get(ModelConstants.DOUBLE_VALUE_COLUMN, Double.class);
                if (doubleV != null) {
                    kvEntry = new DoubleDataEntry(key, doubleV);
                } else {
                    @Nullable Boolean boolV = row.get(ModelConstants.BOOLEAN_VALUE_COLUMN, Boolean.class);
                    if (boolV != null) {
                        kvEntry = new BooleanDataEntry(key, boolV);
                    } else {
                        @Nullable String jsonV = row.get(ModelConstants.JSON_VALUE_COLUMN, String.class);
                        if (StringUtils.isNoneEmpty(jsonV)) {
                            kvEntry = new JsonDataEntry(key, jsonV);
                        } else {
                            log.warn("All values in key-value row are nullable ");
                        }
                    }
                }
            }
        }
        return kvEntry;
    }

    protected List<TsKvEntry> convertResultToTsKvEntryList(List<Row> rows) {
        List<TsKvEntry> entries = new ArrayList<>(rows.size());
        if (!rows.isEmpty()) {
            rows.forEach(row -> entries.add(convertResultToTsKvEntry(row)));
        }
        return entries;
    }

    private TsKvEntry convertResultToTsKvEntry(Row row) {
        @Nullable String key = row.getString(ModelConstants.KEY_COLUMN);
        long ts = row.getLong(ModelConstants.TS_COLUMN);
        return new BasicTsKvEntry(ts, toKvEntry(row, key));
    }

    protected TsKvEntry convertResultToTsKvEntry(String key, @Nullable Row row) {
        if (row != null) {
            return getBasicTsKvEntry(key, row);
        } else {
            return new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key, null));
        }
    }

    protected Optional<TsKvEntry> convertResultToTsKvEntryOpt(String key, @Nullable Row row) {
        if (row != null) {
            return Optional.of(getBasicTsKvEntry(key, row));
        } else {
            return Optional.empty();
        }
    }

    private BasicTsKvEntry getBasicTsKvEntry(String key, Row row) {
        Optional<String> foundKeyOpt = getKey(row);
        long ts = row.getLong(ModelConstants.TS_COLUMN);
        return new BasicTsKvEntry(ts, toKvEntry(row, foundKeyOpt.orElse(key)));
    }

    private Optional<String> getKey(Row row){
       try{
           return Optional.ofNullable(row.getString(ModelConstants.KEY_COLUMN));
       } catch (IllegalArgumentException e){
           return Optional.empty();
       }
    }

}
