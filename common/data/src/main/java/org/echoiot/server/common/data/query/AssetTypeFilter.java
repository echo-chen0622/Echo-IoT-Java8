package org.echoiot.server.common.data.query;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class AssetTypeFilter implements EntityFilter {

    @NotNull
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ASSET_TYPE;
    }

    private String assetType;

    private String assetNameFilter;

}
