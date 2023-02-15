package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;

@Data
@RequiredArgsConstructor
public class EntityData {

    private final EntityId entityId;
    private final Map<EntityKeyType, Map<String, TsValue>> latest;
    private final Map<String, TsValue[]> timeseries;
    private final Map<Integer, ComparisonTsValue> aggLatest;

    public EntityData(EntityId entityId, Map<EntityKeyType, Map<String, TsValue>> latest, Map<String, TsValue[]> timeseries) {
        this(entityId, latest, timeseries, null);
    }

    @JsonIgnore
    public void clearTsAndAggData() {
        if (timeseries != null) {
            timeseries.clear();
        }
        if (aggLatest != null) {
            aggLatest.clear();
        }
    }
}
