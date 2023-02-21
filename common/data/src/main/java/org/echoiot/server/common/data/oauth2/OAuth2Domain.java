package org.echoiot.server.common.data.oauth2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.echoiot.server.common.data.BaseData;
import org.echoiot.server.common.data.id.OAuth2DomainId;
import org.echoiot.server.common.data.id.OAuth2ParamsId;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
public class OAuth2Domain extends BaseData<OAuth2DomainId> {

    private OAuth2ParamsId oauth2ParamsId;
    private String domainName;
    private SchemeType domainScheme;

    public OAuth2Domain(@NotNull OAuth2Domain domain) {
        super(domain);
        this.oauth2ParamsId = domain.oauth2ParamsId;
        this.domainName = domain.domainName;
        this.domainScheme = domain.domainScheme;
    }
}
