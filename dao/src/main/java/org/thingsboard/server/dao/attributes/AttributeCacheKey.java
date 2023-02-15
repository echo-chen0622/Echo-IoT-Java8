package org.thingsboard.server.dao.attributes;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityId;

import java.io.Serializable;

@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class AttributeCacheKey implements Serializable {
    private static final long serialVersionUID = 2013369077925351881L;

    private final String scope;
    private final EntityId entityId;
    private final String key;

    @Override
    public String toString() {
        return "{" + entityId + "}" + scope + "_" + key;
    }
}
