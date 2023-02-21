package org.echoiot.server.dao.model.sql;

import lombok.Data;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.dao.model.ToData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.util.UUID;

import static org.echoiot.server.dao.model.ModelConstants.*;

@Data
@MappedSuperclass
public abstract class AbstractTsKvEntity implements ToData<TsKvEntry> {

    protected static final String SUM = "SUM";
    protected static final String AVG = "AVG";
    protected static final String MIN = "MIN";
    protected static final String MAX = "MAX";

    @Id
    @Column(name = ENTITY_ID_COLUMN, columnDefinition = "uuid")
    protected UUID entityId;

    @Id
    @Column(name = KEY_COLUMN)
    protected int key;

    @Id
    @Column(name = TS_COLUMN)
    protected Long ts;

    @Column(name = BOOLEAN_VALUE_COLUMN)
    protected Boolean booleanValue;

    @Column(name = STRING_VALUE_COLUMN)
    protected String strValue;

    @Column(name = LONG_VALUE_COLUMN)
    protected Long longValue;

    @Column(name = DOUBLE_VALUE_COLUMN)
    protected Double doubleValue;

    @Column(name = JSON_VALUE_COLUMN)
    protected String jsonValue;

    @Transient
    protected String strKey;

    @Transient
    protected Long aggValuesLastTs;
    @Transient
    protected Long aggValuesCount;

    public AbstractTsKvEntity() {
    }

    public AbstractTsKvEntity(Long aggValuesLastTs) {
        this.aggValuesLastTs = aggValuesLastTs;
    }

    public abstract boolean isNotEmpty();

    protected static boolean isAllNull(@NotNull Object... args) {
        for (@Nullable Object arg : args) {
            if (arg != null) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    @Override
    public TsKvEntry toData() {
        @Nullable KvEntry kvEntry = null;
        if (strValue != null) {
            kvEntry = new StringDataEntry(strKey, strValue);
        } else if (longValue != null) {
            kvEntry = new LongDataEntry(strKey, longValue);
        } else if (doubleValue != null) {
            kvEntry = new DoubleDataEntry(strKey, doubleValue);
        } else if (booleanValue != null) {
            kvEntry = new BooleanDataEntry(strKey, booleanValue);
        } else if (jsonValue != null) {
            kvEntry = new JsonDataEntry(strKey, jsonValue);
        }

        if (aggValuesCount == null) {
            return new BasicTsKvEntry(ts, kvEntry);
        } else {
            return new AggTsKvEntry(ts, kvEntry, aggValuesCount);
        }
    }

}
