package org.echoiot.server.common.data.oauth2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.echoiot.server.common.data.BaseData;
import org.echoiot.server.common.data.id.OAuth2ParamsId;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
public class OAuth2Params extends BaseData<OAuth2ParamsId> {

    private boolean enabled;
    private TenantId tenantId;

    public OAuth2Params(@NotNull OAuth2Params oauth2Params) {
        super(oauth2Params);
        this.enabled = oauth2Params.enabled;
        this.tenantId = oauth2Params.tenantId;
    }
}
