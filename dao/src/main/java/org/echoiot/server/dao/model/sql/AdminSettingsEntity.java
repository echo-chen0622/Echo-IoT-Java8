package org.echoiot.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.echoiot.server.common.data.AdminSettings;
import org.echoiot.server.common.data.id.AdminSettingsId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.model.BaseEntity;
import org.echoiot.server.dao.model.BaseSqlEntity;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.util.mapping.JsonStringType;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

import static org.echoiot.server.dao.model.ModelConstants.ADMIN_SETTINGS_COLUMN_FAMILY_NAME;
import static org.echoiot.server.dao.model.ModelConstants.ADMIN_SETTINGS_JSON_VALUE_PROPERTY;
import static org.echoiot.server.dao.model.ModelConstants.ADMIN_SETTINGS_KEY_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ADMIN_SETTINGS_COLUMN_FAMILY_NAME)
public final class AdminSettingsEntity extends BaseSqlEntity<AdminSettings> implements BaseEntity<AdminSettings> {

    @Column(name = ModelConstants.ADMIN_SETTINGS_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ADMIN_SETTINGS_KEY_PROPERTY)
    private String key;

    @Type(type = "json")
    @Column(name = ADMIN_SETTINGS_JSON_VALUE_PROPERTY)
    private JsonNode jsonValue;

    public AdminSettingsEntity() {
        super();
    }

    public AdminSettingsEntity(@NotNull AdminSettings adminSettings) {
        if (adminSettings.getId() != null) {
            this.setUuid(adminSettings.getId().getId());
        }
        this.setCreatedTime(adminSettings.getCreatedTime());
        this.tenantId = adminSettings.getTenantId().getId();
        this.key = adminSettings.getKey();
        this.jsonValue = adminSettings.getJsonValue();
    }

    @NotNull
    @Override
    public AdminSettings toData() {
        @NotNull AdminSettings adminSettings = new AdminSettings(new AdminSettingsId(id));
        adminSettings.setCreatedTime(createdTime);
        adminSettings.setTenantId(TenantId.fromUUID(tenantId));
        adminSettings.setKey(key);
        adminSettings.setJsonValue(jsonValue);
        return adminSettings;
    }

}
