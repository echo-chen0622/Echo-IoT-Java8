package org.echoiot.server.service.security.auth.mfa.provider.impl;

import org.echoiot.rule.engine.api.MailService;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.model.mfa.account.EmailTwoFaAccountConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.EmailTwoFaProviderConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.security.model.SecurityUser;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@TbCoreComponent
public class EmailTwoFaProvider extends OtpBasedTwoFaProvider<EmailTwoFaProviderConfig, EmailTwoFaAccountConfig> {

    private final MailService mailService;

    protected EmailTwoFaProvider(CacheManager cacheManager, MailService mailService) {
        super(cacheManager);
        this.mailService = mailService;
    }

    @Override
    public EmailTwoFaAccountConfig generateNewAccountConfig(User user, EmailTwoFaProviderConfig providerConfig) {
        EmailTwoFaAccountConfig config = new EmailTwoFaAccountConfig();
        config.setEmail(user.getEmail());
        return config;
    }

    @Override
    public void check(TenantId tenantId) throws EchoiotException {
        try {
            mailService.testConnection(tenantId);
        } catch (Exception e) {
            throw new EchoiotException("Mail service is not set up", EchoiotErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    @Override
    protected void sendVerificationCode(SecurityUser user, String verificationCode, EmailTwoFaProviderConfig providerConfig, EmailTwoFaAccountConfig accountConfig) throws EchoiotException {
        mailService.sendTwoFaVerificationEmail(accountConfig.getEmail(), verificationCode, providerConfig.getVerificationCodeLifetime());
    }

    @Override
    public TwoFaProviderType getType() {
        return TwoFaProviderType.EMAIL;
    }

}
