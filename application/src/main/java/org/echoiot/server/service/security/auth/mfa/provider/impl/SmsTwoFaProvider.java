package org.echoiot.server.service.security.auth.mfa.provider.impl;

import org.echoiot.rule.engine.api.SmsService;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.model.mfa.account.SmsTwoFaAccountConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.SmsTwoFaProviderConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.security.model.SecurityUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@TbCoreComponent
public class SmsTwoFaProvider extends OtpBasedTwoFaProvider<SmsTwoFaProviderConfig, SmsTwoFaAccountConfig> {

    private final SmsService smsService;

    public SmsTwoFaProvider(@NotNull CacheManager cacheManager, SmsService smsService) {
        super(cacheManager);
        this.smsService = smsService;
    }


    @NotNull
    @Override
    public SmsTwoFaAccountConfig generateNewAccountConfig(User user, SmsTwoFaProviderConfig providerConfig) {
        return new SmsTwoFaAccountConfig();
    }

    @Override
    protected void sendVerificationCode(@NotNull SecurityUser user, @NotNull String verificationCode, @NotNull SmsTwoFaProviderConfig providerConfig, @NotNull SmsTwoFaAccountConfig accountConfig) throws EchoiotException {
        @NotNull Map<String, String> messageData = Map.of(
                "code", verificationCode,
                "userEmail", user.getEmail()
                                                         );
        String message = TbNodeUtils.processTemplate(providerConfig.getSmsVerificationMessageTemplate(), messageData);
        String phoneNumber = accountConfig.getPhoneNumber();

        smsService.sendSms(user.getTenantId(), user.getCustomerId(), new String[]{phoneNumber}, message);
    }

    @Override
    public void check(TenantId tenantId) throws EchoiotException {
        if (!smsService.isConfigured(tenantId)) {
            throw new EchoiotException("SMS service is not configured", EchoiotErrorCode.BAD_REQUEST_PARAMS);
        }
    }


    @NotNull
    @Override
    public TwoFaProviderType getType() {
        return TwoFaProviderType.SMS;
    }

}
