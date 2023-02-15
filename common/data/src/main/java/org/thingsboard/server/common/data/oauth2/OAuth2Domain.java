package org.thingsboard.server.common.data.oauth2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.OAuth2DomainId;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
public class OAuth2Domain extends BaseData<OAuth2DomainId> {

    private OAuth2ParamsId oauth2ParamsId;
    private String domainName;
    private SchemeType domainScheme;

    public OAuth2Domain(OAuth2Domain domain) {
        super(domain);
        this.oauth2ParamsId = domain.oauth2ParamsId;
        this.domainName = domain.domainName;
        this.domainScheme = domain.domainScheme;
    }
}
