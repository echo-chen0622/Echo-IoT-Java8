package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.OAuth2MobileId;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;
import org.thingsboard.server.common.data.oauth2.OAuth2Mobile;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.OAUTH2_MOBILE_COLUMN_FAMILY_NAME)
public class OAuth2MobileEntity extends BaseSqlEntity<OAuth2Mobile> {

    @Column(name = ModelConstants.OAUTH2_PARAMS_ID_PROPERTY)
    private UUID oauth2ParamsId;

    @Column(name = ModelConstants.OAUTH2_PKG_NAME_PROPERTY)
    private String pkgName;

    @Column(name = ModelConstants.OAUTH2_APP_SECRET_PROPERTY)
    private String appSecret;

    public OAuth2MobileEntity() {
        super();
    }

    public OAuth2MobileEntity(OAuth2Mobile mobile) {
        if (mobile.getId() != null) {
            this.setUuid(mobile.getId().getId());
        }
        this.setCreatedTime(mobile.getCreatedTime());
        if (mobile.getOauth2ParamsId() != null) {
            this.oauth2ParamsId = mobile.getOauth2ParamsId().getId();
        }
        this.pkgName = mobile.getPkgName();
        this.appSecret = mobile.getAppSecret();
    }

    @Override
    public OAuth2Mobile toData() {
        OAuth2Mobile mobile = new OAuth2Mobile();
        mobile.setId(new OAuth2MobileId(id));
        mobile.setCreatedTime(createdTime);
        mobile.setOauth2ParamsId(new OAuth2ParamsId(oauth2ParamsId));
        mobile.setPkgName(pkgName);
        mobile.setAppSecret(appSecret);
        return mobile;
    }
}
