package org.echoiot.server.service.security.auth.mfa;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.echoiot.server.common.data.security.model.mfa.account.TwoFaAccountConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.TwoFaProviderConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.echoiot.server.common.msg.tools.TbRateLimits;
import org.echoiot.server.dao.user.UserService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.echoiot.server.service.security.auth.mfa.provider.TwoFaProvider;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.system.SystemSecurityService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@TbCoreComponent
public class DefaultTwoFactorAuthService implements TwoFactorAuthService {

    @NotNull
    private final TwoFaConfigManager configManager;
    @NotNull
    private final SystemSecurityService systemSecurityService;
    @NotNull
    private final UserService userService;
    private final Map<TwoFaProviderType, TwoFaProvider<TwoFaProviderConfig, TwoFaAccountConfig>> providers = new EnumMap<>(TwoFaProviderType.class);

    private static final EchoiotException ACCOUNT_NOT_CONFIGURED_ERROR = new EchoiotException("2FA is not configured for account", EchoiotErrorCode.BAD_REQUEST_PARAMS);
    private static final EchoiotException PROVIDER_NOT_CONFIGURED_ERROR = new EchoiotException("2FA provider is not configured", EchoiotErrorCode.BAD_REQUEST_PARAMS);
    private static final EchoiotException PROVIDER_NOT_AVAILABLE_ERROR = new EchoiotException("2FA provider is not available", EchoiotErrorCode.GENERAL);

    private final ConcurrentMap<UserId, ConcurrentMap<TwoFaProviderType, TbRateLimits>> verificationCodeSendingRateLimits = new ConcurrentHashMap<>();
    private final ConcurrentMap<UserId, ConcurrentMap<TwoFaProviderType, TbRateLimits>> verificationCodeCheckingRateLimits = new ConcurrentHashMap<>();

    @Override
    public boolean isTwoFaEnabled(TenantId tenantId, UserId userId) {
        return configManager.getAccountTwoFaSettings(tenantId, userId)
                .map(settings -> !settings.getConfigs().isEmpty())
                .orElse(false);
    }

    @Override
    public void checkProvider(TenantId tenantId, TwoFaProviderType providerType) throws EchoiotException {
        getTwoFaProvider(providerType).check(tenantId);
    }


    @Override
    public void prepareVerificationCode(@NotNull SecurityUser user, TwoFaProviderType providerType, boolean checkLimits) throws Exception {
        TwoFaAccountConfig accountConfig = configManager.getTwoFaAccountConfig(user.getTenantId(), user.getId(), providerType)
                .orElseThrow(() -> ACCOUNT_NOT_CONFIGURED_ERROR);
        prepareVerificationCode(user, accountConfig, checkLimits);
    }

    @Override
    public void prepareVerificationCode(@NotNull SecurityUser user, @NotNull TwoFaAccountConfig accountConfig, boolean checkLimits) throws EchoiotException {
        PlatformTwoFaSettings twoFaSettings = configManager.getPlatformTwoFaSettings(user.getTenantId(), true)
                                                           .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);
        if (checkLimits) {
            Integer minVerificationCodeSendPeriod = twoFaSettings.getMinVerificationCodeSendPeriod();
            @Nullable String rateLimit = null;
            if (minVerificationCodeSendPeriod != null && minVerificationCodeSendPeriod > 4) {
                rateLimit = "1:" + minVerificationCodeSendPeriod;
            }
            checkRateLimits(user.getId(), accountConfig.getProviderType(), rateLimit, verificationCodeSendingRateLimits);
        }

        TwoFaProviderConfig providerConfig = twoFaSettings.getProviderConfig(accountConfig.getProviderType())
                .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);
        getTwoFaProvider(accountConfig.getProviderType()).prepareVerificationCode(user, providerConfig, accountConfig);
    }


    @Override
    public boolean checkVerificationCode(@NotNull SecurityUser user, TwoFaProviderType providerType, String verificationCode, boolean checkLimits) throws EchoiotException {
        TwoFaAccountConfig accountConfig = configManager.getTwoFaAccountConfig(user.getTenantId(), user.getId(), providerType)
                .orElseThrow(() -> ACCOUNT_NOT_CONFIGURED_ERROR);
        return checkVerificationCode(user, verificationCode, accountConfig, checkLimits);
    }

    @Override
    public boolean checkVerificationCode(@NotNull SecurityUser user, String verificationCode, @NotNull TwoFaAccountConfig accountConfig, boolean checkLimits) throws EchoiotException {
        if (!userService.findUserCredentialsByUserId(user.getTenantId(), user.getId()).isEnabled()) {
            throw new EchoiotException("User is disabled", EchoiotErrorCode.AUTHENTICATION);
        }

        PlatformTwoFaSettings twoFaSettings = configManager.getPlatformTwoFaSettings(user.getTenantId(), true)
                .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);
        if (checkLimits) {
            checkRateLimits(user.getId(), accountConfig.getProviderType(), twoFaSettings.getVerificationCodeCheckRateLimit(), verificationCodeCheckingRateLimits);
        }
        TwoFaProviderConfig providerConfig = twoFaSettings.getProviderConfig(accountConfig.getProviderType())
                .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);

        boolean verificationSuccess = false;
        if (StringUtils.isNotBlank(verificationCode)) {
            if (StringUtils.isNumeric(verificationCode) || accountConfig.getProviderType() == TwoFaProviderType.BACKUP_CODE) {
                verificationSuccess = getTwoFaProvider(accountConfig.getProviderType()).checkVerificationCode(user, verificationCode, providerConfig, accountConfig);
            }
        }
        if (checkLimits) {
            try {
                systemSecurityService.validateTwoFaVerification(user, verificationSuccess, twoFaSettings);
            } catch (LockedException e) {
                verificationCodeCheckingRateLimits.remove(user.getId());
                verificationCodeSendingRateLimits.remove(user.getId());
                throw new EchoiotException(e.getMessage(), EchoiotErrorCode.AUTHENTICATION);
            }
            if (verificationSuccess) {
                verificationCodeCheckingRateLimits.remove(user.getId());
                verificationCodeSendingRateLimits.remove(user.getId());
            }
        }
        return verificationSuccess;
    }

    private void checkRateLimits(UserId userId, TwoFaProviderType providerType, @NotNull String rateLimitConfig,
                                 @NotNull ConcurrentMap<UserId, ConcurrentMap<TwoFaProviderType, TbRateLimits>> rateLimits) throws EchoiotException {
        if (StringUtils.isNotEmpty(rateLimitConfig)) {
            @NotNull ConcurrentMap<TwoFaProviderType, TbRateLimits> providersRateLimits = rateLimits.computeIfAbsent(userId, i -> new ConcurrentHashMap<>());

            TbRateLimits rateLimit = providersRateLimits.get(providerType);
            if (rateLimit == null || !rateLimit.getConfiguration().equals(rateLimitConfig)) {
                rateLimit = new TbRateLimits(rateLimitConfig, true);
                providersRateLimits.put(providerType, rateLimit);
            }
            if (!rateLimit.tryConsume()) {
                throw new EchoiotException("Too many requests", EchoiotErrorCode.TOO_MANY_REQUESTS);
            }
        } else {
            rateLimits.remove(userId);
        }
    }


    @Override
    public TwoFaAccountConfig generateNewAccountConfig(@NotNull User user, TwoFaProviderType providerType) throws EchoiotException {
        TwoFaProviderConfig providerConfig = getTwoFaProviderConfig(user.getTenantId(), providerType);
        return getTwoFaProvider(providerType).generateNewAccountConfig(user, providerConfig);
    }


    private TwoFaProviderConfig getTwoFaProviderConfig(TenantId tenantId, TwoFaProviderType providerType) throws EchoiotException {
        return configManager.getPlatformTwoFaSettings(tenantId, true)
                .flatMap(twoFaSettings -> twoFaSettings.getProviderConfig(providerType))
                .orElseThrow(() -> PROVIDER_NOT_CONFIGURED_ERROR);
    }

    private TwoFaProvider<TwoFaProviderConfig, TwoFaAccountConfig> getTwoFaProvider(TwoFaProviderType providerType) throws EchoiotException {
        return Optional.ofNullable(providers.get(providerType))
                .orElseThrow(() -> PROVIDER_NOT_AVAILABLE_ERROR);
    }

    @Resource
    private void setProviders(@NotNull Collection<TwoFaProvider> providers) {
        providers.forEach(provider -> {
            this.providers.put(provider.getType(), provider);
        });
    }

}
