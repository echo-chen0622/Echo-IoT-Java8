package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.dao.util.mapping.JsonStringType;
import org.hibernate.annotations.TypeDef;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Table;

import static org.echoiot.server.dao.model.ModelConstants.ASSET_COLUMN_FAMILY_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ASSET_COLUMN_FAMILY_NAME)
public final class AssetEntity extends AbstractAssetEntity<Asset> {

    public AssetEntity() {
        super();
    }

    public AssetEntity(@NotNull Asset asset) {
        super(asset);
    }

    @Override
    public Asset toData() {
        return super.toAsset();
    }

}
