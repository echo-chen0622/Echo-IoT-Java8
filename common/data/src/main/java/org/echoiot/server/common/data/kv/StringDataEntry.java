package org.echoiot.server.common.data.kv;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public class StringDataEntry extends BasicKvEntry {

    private static final long serialVersionUID = 1L;

    private final String value;

    public StringDataEntry(String key, String value) {
        super(key);
        this.value = value;
    }

    @NotNull
    @Override
    public DataType getDataType() {
        return DataType.STRING;
    }

    @NotNull
    @Override
    public Optional<String> getStrValue() {
        return Optional.ofNullable(value);
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof StringDataEntry))
            return false;
        if (!super.equals(o))
            return false;
        @NotNull StringDataEntry that = (StringDataEntry) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    @NotNull
    @Override
    public String toString() {
        return "StringDataEntry{" + "value='" + value + '\'' + "} " + super.toString();
    }

    @Override
    public String getValueAsString() {
        return value;
    }
}
