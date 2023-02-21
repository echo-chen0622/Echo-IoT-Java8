package org.echoiot.server.common.data.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AssetSearchQueryFilter extends EntitySearchQueryFilter {

    @NotNull
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.ASSET_SEARCH_QUERY;
    }

    private List<String> assetTypes;

}
