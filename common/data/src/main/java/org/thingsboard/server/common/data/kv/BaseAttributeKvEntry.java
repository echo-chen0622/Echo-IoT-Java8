package org.thingsboard.server.common.data.kv;

import javax.validation.Valid;
import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
public class BaseAttributeKvEntry implements AttributeKvEntry {

    private static final long serialVersionUID = -6460767583563159407L;

    private final long lastUpdateTs;
    @Valid
    private final KvEntry kv;

    public BaseAttributeKvEntry(KvEntry kv, long lastUpdateTs) {
        this.kv = kv;
        this.lastUpdateTs = lastUpdateTs;
    }

    public BaseAttributeKvEntry(long lastUpdateTs, KvEntry kv) {
        this(kv, lastUpdateTs);
    }

    @Override
    public long getLastUpdateTs() {
        return lastUpdateTs;
    }

    @Override
    public String getKey() {
        return kv.getKey();
    }

    @Override
    public DataType getDataType() {
        return kv.getDataType();
    }

    @Override
    public Optional<String> getStrValue() {
        return kv.getStrValue();
    }

    @Override
    public Optional<Long> getLongValue() {
        return kv.getLongValue();
    }

    @Override
    public Optional<Boolean> getBooleanValue() {
        return kv.getBooleanValue();
    }

    @Override
    public Optional<Double> getDoubleValue() {
        return kv.getDoubleValue();
    }

    @Override
    public Optional<String> getJsonValue() {
        return kv.getJsonValue();
    }

    @Override
    public String getValueAsString() {
        return kv.getValueAsString();
    }

    @Override
    public Object getValue() {
        return kv.getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseAttributeKvEntry that = (BaseAttributeKvEntry) o;

        if (lastUpdateTs != that.lastUpdateTs) return false;
        return kv.equals(that.kv);

    }

    @Override
    public int hashCode() {
        int result = (int) (lastUpdateTs ^ (lastUpdateTs >>> 32));
        result = 31 * result + kv.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BaseAttributeKvEntry{" +
                "lastUpdateTs=" + lastUpdateTs +
                ", kv=" + kv +
                '}';
    }
}
