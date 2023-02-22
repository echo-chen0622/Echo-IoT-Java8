package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.id.OAuth2DomainId;
import org.echoiot.server.common.data.id.OAuth2ParamsId;
import org.echoiot.server.common.data.oauth2.OAuth2Domain;
import org.echoiot.server.common.data.oauth2.SchemeType;
import org.echoiot.server.dao.model.BaseSqlEntity;
import org.echoiot.server.dao.model.ModelConstants;

import javax.persistence.*;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.OAUTH2_DOMAIN_COLUMN_FAMILY_NAME)
public class OAuth2DomainEntity extends BaseSqlEntity<OAuth2Domain> {

    @Column(name = ModelConstants.OAUTH2_PARAMS_ID_PROPERTY)
    private UUID oauth2ParamsId;

    @Column(name = ModelConstants.OAUTH2_DOMAIN_NAME_PROPERTY)
    private String domainName;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.OAUTH2_DOMAIN_SCHEME_PROPERTY)
    private SchemeType domainScheme;

    public OAuth2DomainEntity() {
        super();
    }

    public OAuth2DomainEntity(OAuth2Domain domain) {
        if (domain.getId() != null) {
            this.setUuid(domain.getId().getId());
        }
        this.setCreatedTime(domain.getCreatedTime());
        if (domain.getOauth2ParamsId() != null) {
            this.oauth2ParamsId = domain.getOauth2ParamsId().getId();
        }
        this.domainName = domain.getDomainName();
        this.domainScheme = domain.getDomainScheme();
    }

    @Override
    public OAuth2Domain toData() {
        OAuth2Domain domain = new OAuth2Domain();
        domain.setId(new OAuth2DomainId(id));
        domain.setCreatedTime(createdTime);
        domain.setOauth2ParamsId(new OAuth2ParamsId(oauth2ParamsId));
        domain.setDomainName(domainName);
        domain.setDomainScheme(domainScheme);
        return domain;
    }
}
