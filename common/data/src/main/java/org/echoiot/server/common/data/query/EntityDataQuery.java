package org.echoiot.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@ToString(callSuper = true)
public class EntityDataQuery extends AbstractDataQuery<EntityDataPageLink> {

    public EntityDataQuery() {
    }

    public EntityDataQuery(EntityFilter entityFilter, List<KeyFilter> keyFilters) {
        super(entityFilter, keyFilters);
    }

    public EntityDataQuery(EntityFilter entityFilter, EntityDataPageLink pageLink, List<EntityKey> entityFields, List<EntityKey> latestValues, List<KeyFilter> keyFilters) {
        super(entityFilter, pageLink, entityFields, latestValues, keyFilters);
    }

    @NotNull
    @JsonIgnore
    public EntityDataQuery next() {
        return new EntityDataQuery(getEntityFilter(), getPageLink().nextPageLink(), entityFields, latestValues, keyFilters);
    }

}
