package org.echoiot.server.common.data.asset;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.AssetProfileId;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.validation.Length;
import org.echoiot.server.common.data.validation.NoXss;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@ApiModel
@EqualsAndHashCode(callSuper = true)
public class Asset extends SearchTextBasedWithAdditionalInfo<AssetId> implements HasName, HasTenantId, HasCustomerId, ExportableEntity<AssetId> {

    private static final long serialVersionUID = 2807343040519543363L;

    private TenantId tenantId;
    private CustomerId customerId;
    @NoXss
    @Length(fieldName = "name")
    private String name;
    @NoXss
    @Length(fieldName = "type")
    private String type;
    @NoXss
    @Length(fieldName = "label")
    private String label;

    private AssetProfileId assetProfileId;

    @Getter @Setter
    private AssetId externalId;

    public Asset() {
        super();
    }

    public Asset(AssetId id) {
        super(id);
    }

    public Asset(@NotNull Asset asset) {
        super(asset);
        this.tenantId = asset.getTenantId();
        this.customerId = asset.getCustomerId();
        this.name = asset.getName();
        this.type = asset.getType();
        this.label = asset.getLabel();
        this.assetProfileId = asset.getAssetProfileId();
        this.externalId = asset.getExternalId();
    }

    public void update(@NotNull Asset asset) {
        this.tenantId = asset.getTenantId();
        this.customerId = asset.getCustomerId();
        this.name = asset.getName();
        this.type = asset.getType();
        this.label = asset.getLabel();
        this.assetProfileId = asset.getAssetProfileId();
        Optional.ofNullable(asset.getAdditionalInfo()).ifPresent(this::setAdditionalInfo);
        this.externalId = asset.getExternalId();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the asset Id. " +
            "Specify this field to update the asset. " +
            "Referencing non-existing asset Id will cause error. " +
            "Omit this field to create new asset.")
    @Override
    public AssetId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the asset creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(position = 4, value = "JSON object with Customer Id. Use 'assignAssetToCustomer' to change the Customer Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public CustomerId getCustomerId() {
        return customerId;
    }

    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    @ApiModelProperty(position = 5, required = true, value = "Unique Asset Name in scope of Tenant", example = "Empire State Building")
    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(position = 6, required = true, value = "Asset type", example = "Building")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ApiModelProperty(position = 7, required = true, value = "Label that may be used in widgets", example = "NY Building")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @ApiModelProperty(position = 8, required = true, value = "JSON object with Asset Profile Id.")
    public AssetProfileId getAssetProfileId() {
        return assetProfileId;
    }

    public void setAssetProfileId(AssetProfileId assetProfileId) {
        this.assetProfileId = assetProfileId;
    }


    @Override
    public String getSearchText() {
        return getName();
    }

    @ApiModelProperty(position = 9, value = "Additional parameters of the asset", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    @Override
    public String toString() {
        String builder = "Asset [tenantId=" +
                         tenantId +
                         ", customerId=" +
                         customerId +
                         ", name=" +
                         name +
                         ", type=" +
                         type +
                         ", label=" +
                         label +
                         ", assetProfileId=" +
                         assetProfileId +
                         ", additionalInfo=" +
                         getAdditionalInfo() +
                         ", createdTime=" +
                         createdTime +
                         ", id=" +
                         id +
                         "]";
        return builder;
    }

}
