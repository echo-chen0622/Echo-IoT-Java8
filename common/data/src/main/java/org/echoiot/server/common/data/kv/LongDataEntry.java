package org.echoiot.server.common.data.kv;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public class LongDataEntry extends BasicKvEntry {

    private final Long value;

    public LongDataEntry(String key, Long value) {
        super(key);
        this.value = value;
    }

    @NotNull
    @Override
    public DataType getDataType() {
        return DataType.LONG;
    }

    @NotNull
    @Override
    public Optional<Long> getLongValue() {
        return Optional.ofNullable(value);
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LongDataEntry)) return false;
        if (!super.equals(o)) return false;
        @NotNull LongDataEntry that = (LongDataEntry) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    @NotNull
    @Override
    public String toString() {
        return "LongDataEntry{" +
                "value=" + value +
                "} " + super.toString();
    }

    @NotNull
    @Override
    public String getValueAsString() {
        return Long.toString(value);
    }
}
