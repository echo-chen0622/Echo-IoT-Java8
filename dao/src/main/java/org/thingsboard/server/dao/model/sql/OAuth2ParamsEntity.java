package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Params;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.OAUTH2_PARAMS_COLUMN_FAMILY_NAME)
@NoArgsConstructor
public class OAuth2ParamsEntity extends BaseSqlEntity<OAuth2Params> {

    @Column(name = ModelConstants.OAUTH2_PARAMS_ENABLED_PROPERTY)
    private Boolean enabled;

    @Column(name = ModelConstants.OAUTH2_PARAMS_TENANT_ID_PROPERTY)
    private UUID tenantId;

    public OAuth2ParamsEntity(OAuth2Params oauth2Params) {
        if (oauth2Params.getId() != null) {
            this.setUuid(oauth2Params.getUuidId());
        }
        this.setCreatedTime(oauth2Params.getCreatedTime());
        this.enabled = oauth2Params.isEnabled();
        if (oauth2Params.getTenantId() != null) {
            this.tenantId = oauth2Params.getTenantId().getId();
        }
    }

    @Override
    public OAuth2Params toData() {
        OAuth2Params oauth2Params = new OAuth2Params();
        oauth2Params.setId(new OAuth2ParamsId(id));
        oauth2Params.setCreatedTime(createdTime);
        oauth2Params.setTenantId(TenantId.fromUUID(tenantId));
        oauth2Params.setEnabled(enabled);
        return oauth2Params;
    }
}
