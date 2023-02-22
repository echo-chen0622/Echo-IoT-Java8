package org.echoiot.server.dao.model.sql;

import lombok.Data;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.dao.model.ToData;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

import static org.echoiot.server.dao.model.ModelConstants.*;

@Data
@Entity
@Table(name = "attribute_kv")
public class AttributeKvEntity implements ToData<AttributeKvEntry>, Serializable {

    @EmbeddedId
    private AttributeKvCompositeKey id;

    @Column(name = BOOLEAN_VALUE_COLUMN)
    private Boolean booleanValue;

    @Column(name = STRING_VALUE_COLUMN)
    private String strValue;

    @Column(name = LONG_VALUE_COLUMN)
    private Long longValue;

    @Column(name = DOUBLE_VALUE_COLUMN)
    private Double doubleValue;

    @Column(name = JSON_VALUE_COLUMN)
    private String jsonValue;

    @Column(name = LAST_UPDATE_TS_COLUMN)
    private Long lastUpdateTs;

    @Override
    public AttributeKvEntry toData() {
        @Nullable KvEntry kvEntry = null;
        if (strValue != null) {
            kvEntry = new StringDataEntry(id.getAttributeKey(), strValue);
        } else if (booleanValue != null) {
            kvEntry = new BooleanDataEntry(id.getAttributeKey(), booleanValue);
        } else if (doubleValue != null) {
            kvEntry = new DoubleDataEntry(id.getAttributeKey(), doubleValue);
        } else if (longValue != null) {
            kvEntry = new LongDataEntry(id.getAttributeKey(), longValue);
        } else if (jsonValue != null) {
            kvEntry = new JsonDataEntry(id.getAttributeKey(), jsonValue);
        }

        return new BaseAttributeKvEntry(kvEntry, lastUpdateTs);
    }
}
