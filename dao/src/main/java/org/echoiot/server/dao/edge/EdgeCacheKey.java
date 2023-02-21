package org.echoiot.server.dao.edge;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@Builder
public class EdgeCacheKey implements Serializable {

    @NotNull
    private final TenantId tenantId;
    @NotNull
    private final String name;

    @NotNull
    @Override
    public String toString() {
        return tenantId + "_" + name;
    }

}
