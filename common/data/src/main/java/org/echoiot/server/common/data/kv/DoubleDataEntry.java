package org.echoiot.server.common.data.kv;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public class DoubleDataEntry extends BasicKvEntry {

    private final Double value;

    public DoubleDataEntry(String key, Double value) {
        super(key);
        this.value = value;
    }

    @NotNull
    @Override
    public DataType getDataType() {
        return DataType.DOUBLE;
    }

    @NotNull
    @Override
    public Optional<Double> getDoubleValue() {
        return Optional.ofNullable(value);
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoubleDataEntry)) return false;
        if (!super.equals(o)) return false;
        @NotNull DoubleDataEntry that = (DoubleDataEntry) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    @NotNull
    @Override
    public String toString() {
        return "DoubleDataEntry{" +
                "value=" + value +
                "} " + super.toString();
    }

    @Override
    public String getValueAsString() {
        return Double.toString(value);
    }
}
