package org.thingsboard.server.dao.asset;

import lombok.Data;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by ashvayka on 02.05.17.
 */
@Data
public class AssetTypeFilter {
    @Nullable
    private String relationType;
    @Nullable
    private List<String> assetTypes;
}
