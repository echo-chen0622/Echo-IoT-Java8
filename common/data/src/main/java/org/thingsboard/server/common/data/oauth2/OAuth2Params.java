package org.thingsboard.server.common.data.oauth2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;
import org.thingsboard.server.common.data.id.TenantId;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
public class OAuth2Params extends BaseData<OAuth2ParamsId> {

    private boolean enabled;
    private TenantId tenantId;

    public OAuth2Params(OAuth2Params oauth2Params) {
        super(oauth2Params);
        this.enabled = oauth2Params.enabled;
        this.tenantId = oauth2Params.tenantId;
    }
}
