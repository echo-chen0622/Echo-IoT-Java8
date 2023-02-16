package org.echoiot.server.common.data.security;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.security.model.mfa.account.AccountTwoFaSettings;
import org.echoiot.server.common.data.BaseData;
import org.echoiot.server.common.data.id.UserAuthSettingsId;
import org.echoiot.server.common.data.id.UserId;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserAuthSettings extends BaseData<UserAuthSettingsId> {

    private static final long serialVersionUID = 2628320657987010348L;

    private UserId userId;
    private AccountTwoFaSettings twoFaSettings;

}
