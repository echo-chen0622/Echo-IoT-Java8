package org.echoiot.server.dao.attributes;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.echoiot.server.common.data.id.EntityId;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class AttributeCacheKey implements Serializable {
    private static final long serialVersionUID = 2013369077925351881L;

    @NotNull
    private final String scope;
    @NotNull
    private final EntityId entityId;
    @NotNull
    private final String key;

    @NotNull
    @Override
    public String toString() {
        return "{" + entityId + "}" + scope + "_" + key;
    }
}
