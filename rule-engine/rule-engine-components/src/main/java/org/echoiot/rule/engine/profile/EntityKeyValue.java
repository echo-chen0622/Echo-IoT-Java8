package org.echoiot.rule.engine.profile;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.echoiot.server.common.data.kv.DataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode
class EntityKeyValue {

    @Getter
    private DataType dataType;
    private Long lngValue;
    private Double dblValue;
    private Boolean boolValue;
    private String strValue;

    @Nullable
    public Long getLngValue() {
        return dataType == DataType.LONG ? lngValue : null;
    }

    public void setLngValue(Long lngValue) {
        this.dataType = DataType.LONG;
        this.lngValue = lngValue;
    }

    @Nullable
    public Double getDblValue() {
        return dataType == DataType.DOUBLE ? dblValue : null;
    }

    public void setDblValue(Double dblValue) {
        this.dataType = DataType.DOUBLE;
        this.dblValue = dblValue;
    }

    @Nullable
    public Boolean getBoolValue() {
        return dataType == DataType.BOOLEAN ? boolValue : null;
    }

    public void setBoolValue(Boolean boolValue) {
        this.dataType = DataType.BOOLEAN;
        this.boolValue = boolValue;
    }

    @Nullable
    public String getStrValue() {
        return dataType == DataType.STRING ? strValue : null;
    }

    public void setStrValue(String strValue) {
        this.dataType = DataType.STRING;
        this.strValue = strValue;
    }

    public void setJsonValue(String jsonValue) {
        this.dataType = DataType.JSON;
        this.strValue = jsonValue;
    }

    @Nullable
    public String getJsonValue() {
        return dataType == DataType.JSON ? strValue : null;
    }

    boolean isSet() {
        return dataType != null;
    }

    @NotNull
    static EntityKeyValue fromString(String s) {
        @NotNull EntityKeyValue result = new EntityKeyValue();
        result.setStrValue(s);
        return result;
    }

    @NotNull
    static EntityKeyValue fromBool(boolean b) {
        @NotNull EntityKeyValue result = new EntityKeyValue();
        result.setBoolValue(b);
        return result;
    }

    @NotNull
    static EntityKeyValue fromLong(long l) {
        @NotNull EntityKeyValue result = new EntityKeyValue();
        result.setLngValue(l);
        return result;
    }

    @NotNull
    static EntityKeyValue fromDouble(double d) {
        @NotNull EntityKeyValue result = new EntityKeyValue();
        result.setDblValue(d);
        return result;
    }

    @NotNull
    static EntityKeyValue fromJson(String s) {
        @NotNull EntityKeyValue result = new EntityKeyValue();
        result.setJsonValue(s);
        return result;
    }

}
