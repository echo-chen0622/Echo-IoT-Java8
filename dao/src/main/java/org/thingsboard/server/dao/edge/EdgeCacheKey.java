package org.thingsboard.server.dao.edge;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@Builder
public class EdgeCacheKey implements Serializable {

    private final TenantId tenantId;
    private final String name;

    @Override
    public String toString() {
        return tenantId + "_" + name;
    }

}
