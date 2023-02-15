package org.thingsboard.server.common.data.query;

import lombok.Data;

@Data
public class AssetTypeFilter implements EntityFilter {

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ASSET_TYPE;
    }

    private String assetType;

    private String assetNameFilter;

}
