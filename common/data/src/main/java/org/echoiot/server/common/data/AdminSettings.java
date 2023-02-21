package org.echoiot.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.echoiot.server.common.data.id.AdminSettingsId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.validation.Length;
import org.echoiot.server.common.data.validation.NoXss;
import org.jetbrains.annotations.NotNull;

@ApiModel
public class AdminSettings extends BaseData<AdminSettingsId> implements HasTenantId {

    private static final long serialVersionUID = -7670322981725511892L;

    private TenantId tenantId;

    @NoXss
    @Length(fieldName = "key")
    private String key;
    private transient JsonNode jsonValue;

    public AdminSettings() {
        super();
    }

    public AdminSettings(AdminSettingsId id) {
        super(id);
    }

    public AdminSettings(@NotNull AdminSettings adminSettings) {
        super(adminSettings);
        this.tenantId = adminSettings.getTenantId();
        this.key = adminSettings.getKey();
        this.jsonValue = adminSettings.getJsonValue();
    }

    @ApiModelProperty(position = 1, value = "The Id of the Administration Settings, auto-generated, UUID")
    @Override
    public AdminSettingsId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the settings creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
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

    @ApiModelProperty(position = 4, value = "The Administration Settings key, (e.g. 'general' or 'mail')", example = "mail")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @ApiModelProperty(position = 5, value = "JSON representation of the Administration Settings value")
    public JsonNode getJsonValue() {
        return jsonValue;
    }

    public void setJsonValue(JsonNode jsonValue) {
        this.jsonValue = jsonValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((jsonValue == null) ? 0 : jsonValue.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean equals(@NotNull Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        @NotNull AdminSettings other = (AdminSettings) obj;
        if (jsonValue == null) {
            if (other.jsonValue != null)
                return false;
        } else if (!jsonValue.equals(other.jsonValue))
            return false;
        if (key == null) {
            return other.key == null;
        } else return key.equals(other.key);
    }

    @NotNull
    @Override
    public String toString() {
        String builder = "AdminSettings [key=" + key + ", jsonValue=" + jsonValue + ", createdTime=" + createdTime + ", id=" + id + "]";
        return builder;
    }

}
