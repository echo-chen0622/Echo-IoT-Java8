package org.echoiot.server.dao.asset;

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
public class AssetCacheKey implements Serializable {

    private static final long serialVersionUID = 4196610233744512673L;

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
