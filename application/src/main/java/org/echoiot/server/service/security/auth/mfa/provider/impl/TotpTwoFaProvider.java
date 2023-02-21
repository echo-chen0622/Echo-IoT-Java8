package org.echoiot.server.service.security.auth.mfa.provider.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.client.utils.URIBuilder;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.TotpTwoFaProviderConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.security.auth.mfa.provider.TwoFaProvider;
import org.echoiot.server.service.security.model.SecurityUser;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@TbCoreComponent
public class TotpTwoFaProvider implements TwoFaProvider<TotpTwoFaProviderConfig, TotpTwoFaAccountConfig> {

    @NotNull
    @Override
    public final TotpTwoFaAccountConfig generateNewAccountConfig(@NotNull User user, @NotNull TotpTwoFaProviderConfig providerConfig) {
        @NotNull TotpTwoFaAccountConfig config = new TotpTwoFaAccountConfig();
        String secretKey = generateSecretKey();
        config.setAuthUrl(getTotpAuthUrl(user, secretKey, providerConfig));
        return config;
    }

    @Override
    public final boolean checkVerificationCode(SecurityUser user, String code, TotpTwoFaProviderConfig providerConfig, @NotNull TotpTwoFaAccountConfig accountConfig) {
        @Nullable String secretKey = UriComponentsBuilder.fromUriString(accountConfig.getAuthUrl()).build().getQueryParams().getFirst("secret");
        return new Totp(secretKey).verify(code);
    }

    @SneakyThrows
    private String getTotpAuthUrl(@NotNull User user, String secretKey, @NotNull TotpTwoFaProviderConfig providerConfig) {
        URIBuilder uri = new URIBuilder()
                .setScheme("otpauth")
                .setHost("totp")
                .setParameter("issuer", providerConfig.getIssuerName())
                .setPath("/" + providerConfig.getIssuerName() + ":" + user.getEmail())
                .setParameter("secret", secretKey);
        return uri.build().toASCIIString();
    }

    private String generateSecretKey() {
        return Base32.encode(RandomUtils.nextBytes(20));
    }


    @NotNull
    @Override
    public TwoFaProviderType getType() {
        return TwoFaProviderType.TOTP;
    }

}
