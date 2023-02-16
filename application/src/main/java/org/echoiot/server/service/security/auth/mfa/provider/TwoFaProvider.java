package org.echoiot.server.service.security.auth.mfa.provider;

import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.model.mfa.account.TwoFaAccountConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.TwoFaProviderConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.echoiot.server.service.security.model.SecurityUser;

public interface TwoFaProvider<C extends TwoFaProviderConfig, A extends TwoFaAccountConfig> {

    A generateNewAccountConfig(User user, C providerConfig);

    default void prepareVerificationCode(SecurityUser user, C providerConfig, A accountConfig) throws EchoiotException {}

    boolean checkVerificationCode(SecurityUser user, String code, C providerConfig, A accountConfig);

    default void check(TenantId tenantId) throws EchoiotException {};


    TwoFaProviderType getType();

}