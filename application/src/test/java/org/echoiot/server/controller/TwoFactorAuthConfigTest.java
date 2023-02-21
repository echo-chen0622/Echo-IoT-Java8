package org.echoiot.server.controller;

import org.echoiot.rule.engine.api.SmsService;
import org.echoiot.server.common.data.CacheConstants;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.echoiot.server.common.data.security.model.mfa.account.AccountTwoFaSettings;
import org.echoiot.server.common.data.security.model.mfa.account.SmsTwoFaAccountConfig;
import org.echoiot.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;
import org.echoiot.server.common.data.security.model.mfa.account.TwoFaAccountConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.SmsTwoFaProviderConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.TotpTwoFaProviderConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.TwoFaProviderConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.echoiot.server.service.security.auth.mfa.TwoFactorAuthService;
import org.echoiot.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.echoiot.server.service.security.auth.mfa.provider.impl.OtpBasedTwoFaProvider;
import org.echoiot.server.service.security.auth.mfa.provider.impl.TotpTwoFaProvider;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class TwoFactorAuthConfigTest extends AbstractControllerTest {

    @SpyBean
    private TotpTwoFaProvider totpTwoFactorAuthProvider;
    @MockBean
    private SmsService smsService;
    @Resource
    private CacheManager cacheManager;
    @Resource
    private TwoFaConfigManager twoFaConfigManager;
    @SpyBean
    private TwoFactorAuthService twoFactorAuthService;

    @Before
    public void beforeEach() throws Exception {
        doNothing().when(twoFactorAuthService).checkProvider(any(), any());
        loginSysAdmin();
    }

    @After
    public void afterEach() {
        twoFaConfigManager.deletePlatformTwoFaSettings(TenantId.SYS_TENANT_ID);
        twoFaConfigManager.deletePlatformTwoFaSettings(tenantId);
    }


    @Test
    public void testSavePlatformTwoFaSettings() throws Exception {
        loginSysAdmin();

        @NotNull TotpTwoFaProviderConfig totpTwoFaProviderConfig = new TotpTwoFaProviderConfig();
        totpTwoFaProviderConfig.setIssuerName("tb");
        @NotNull SmsTwoFaProviderConfig smsTwoFaProviderConfig = new SmsTwoFaProviderConfig();
        smsTwoFaProviderConfig.setSmsVerificationMessageTemplate("${code}");
        smsTwoFaProviderConfig.setVerificationCodeLifetime(60);

        @NotNull PlatformTwoFaSettings twoFaSettings = new PlatformTwoFaSettings();
        twoFaSettings.setProviders(List.of(totpTwoFaProviderConfig, smsTwoFaProviderConfig));
        twoFaSettings.setMinVerificationCodeSendPeriod(5);
        twoFaSettings.setVerificationCodeCheckRateLimit("3:900");
        twoFaSettings.setMaxVerificationFailuresBeforeUserLockout(10);
        twoFaSettings.setTotalAllowedTimeForVerification(3600);

        doPost("/api/2fa/settings", twoFaSettings).andExpect(status().isOk());

        @NotNull PlatformTwoFaSettings savedTwoFaSettings = readResponse(doGet("/api/2fa/settings").andExpect(status().isOk()), PlatformTwoFaSettings.class);

        assertThat(savedTwoFaSettings.getProviders()).hasSize(2);
        assertThat(savedTwoFaSettings.getProviders()).contains(totpTwoFaProviderConfig, smsTwoFaProviderConfig);
    }

    @Test
    public void testSavePlatformTwoFaSettings_validationError() throws Exception {
        loginSysAdmin();

        @NotNull PlatformTwoFaSettings twoFaSettings = new PlatformTwoFaSettings();
        twoFaSettings.setProviders(Collections.emptyList());
        twoFaSettings.setVerificationCodeCheckRateLimit("0:12");
        twoFaSettings.setMaxVerificationFailuresBeforeUserLockout(-1);
        twoFaSettings.setTotalAllowedTimeForVerification(0);
        twoFaSettings.setMinVerificationCodeSendPeriod(5);

        String errorMessage = getErrorMessage(doPost("/api/2fa/settings", twoFaSettings)
                .andExpect(status().isBadRequest()));

        assertThat(errorMessage).contains(
                "verification code check rate limit configuration is invalid",
                "maximum number of verification failure before user lockout must be positive",
                "total amount of time allotted for verification must be greater than or equal 60"
        );
    }

    @Test
    public void testSaveTotpTwoFaProviderConfig_validationError() throws Exception {
        @NotNull TotpTwoFaProviderConfig invalidTotpTwoFaProviderConfig = new TotpTwoFaProviderConfig();
        invalidTotpTwoFaProviderConfig.setIssuerName("   ");

        String errorResponse = savePlatformTwoFaSettingsAndGetError(invalidTotpTwoFaProviderConfig);
        assertThat(errorResponse).containsIgnoringCase("issuer name must not be blank");
    }

    @Test
    public void testSaveSmsTwoFaProviderConfig_validationError() throws Exception {
        @NotNull SmsTwoFaProviderConfig invalidSmsTwoFaProviderConfig = new SmsTwoFaProviderConfig();
        invalidSmsTwoFaProviderConfig.setSmsVerificationMessageTemplate("does not contain verification code");
        invalidSmsTwoFaProviderConfig.setVerificationCodeLifetime(60);

        String errorResponse = savePlatformTwoFaSettingsAndGetError(invalidSmsTwoFaProviderConfig);
        assertThat(errorResponse).containsIgnoringCase("must contain verification code");

        invalidSmsTwoFaProviderConfig.setSmsVerificationMessageTemplate(null);
        invalidSmsTwoFaProviderConfig.setVerificationCodeLifetime(0);
        errorResponse = savePlatformTwoFaSettingsAndGetError(invalidSmsTwoFaProviderConfig);
        assertThat(errorResponse).containsIgnoringCase("verification message template is required");
        assertThat(errorResponse).containsIgnoringCase("verification code lifetime is required");
    }

    private String savePlatformTwoFaSettingsAndGetError(TwoFaProviderConfig invalidTwoFaProviderConfig) throws Exception {
        @NotNull PlatformTwoFaSettings twoFaSettings = new PlatformTwoFaSettings();
        twoFaSettings.setProviders(Collections.singletonList(invalidTwoFaProviderConfig));
        twoFaSettings.setMinVerificationCodeSendPeriod(5);
        twoFaSettings.setTotalAllowedTimeForVerification(100);

        return getErrorMessage(doPost("/api/2fa/settings", twoFaSettings)
                .andExpect(status().isBadRequest()));
    }


    @Test
    public void testSaveTwoFaAccountConfig_providerNotConfigured() throws Exception {
        configureSmsTwoFaProvider("${code}");

        loginTenantAdmin();

        @NotNull TwoFaProviderType notConfiguredProviderType = TwoFaProviderType.TOTP;
        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config/generate?providerType=" + notConfiguredProviderType)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("provider is not configured");

        @NotNull TotpTwoFaAccountConfig notConfiguredProviderAccountConfig = new TotpTwoFaAccountConfig();
        notConfiguredProviderAccountConfig.setAuthUrl("otpauth://totp/aba:aba?issuer=aba&secret=ABA");
        errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", notConfiguredProviderAccountConfig));
        assertThat(errorMessage).containsIgnoringCase("provider is not configured");
    }

    @Test
    public void testGenerateTotpTwoFaAccountConfig() throws Exception {
        @NotNull TotpTwoFaProviderConfig totpTwoFaProviderConfig = configureTotpTwoFaProvider();

        loginTenantAdmin();

        assertThat(readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()), String.class)).isNullOrEmpty();
        generateTotpTwoFaAccountConfig(totpTwoFaProviderConfig);
    }

    @Test
    public void testSubmitTotpTwoFaAccountConfig() throws Exception {
        @NotNull TotpTwoFaProviderConfig totpTwoFaProviderConfig = configureTotpTwoFaProvider();

        loginTenantAdmin();

        TotpTwoFaAccountConfig generatedTotpTwoFaAccountConfig = generateTotpTwoFaAccountConfig(totpTwoFaProviderConfig);
        doPost("/api/2fa/account/config/submit", generatedTotpTwoFaAccountConfig).andExpect(status().isOk());
        verify(totpTwoFactorAuthProvider).prepareVerificationCode(argThat(user -> user.getEmail().equals(TENANT_ADMIN_EMAIL)),
                eq(totpTwoFaProviderConfig), eq(generatedTotpTwoFaAccountConfig));
    }

    @Test
    public void testSubmitTotpTwoFaAccountConfig_validationError() throws Exception {
        configureTotpTwoFaProvider();

        loginTenantAdmin();

        @NotNull TotpTwoFaAccountConfig totpTwoFaAccountConfig = new TotpTwoFaAccountConfig();
        totpTwoFaAccountConfig.setAuthUrl(null);

        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", totpTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("otp auth url cannot be blank");

        totpTwoFaAccountConfig.setAuthUrl("otpauth://totp/T B: aba");
        errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", totpTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("otp auth url is invalid");

        totpTwoFaAccountConfig.setAuthUrl("otpauth://totp/Echoiot%20(Tenant):tenant@echoiot.org?issuer=Echoiot+%28Tenant%29&secret=FUNBIM3CXFNNGQR6ZIPVWHP65PPFWDII");
        doPost("/api/2fa/account/config/submit", totpTwoFaAccountConfig)
                .andExpect(status().isOk());
    }

    @Test
    public void testVerifyAndSaveTotpTwoFaAccountConfig() throws Exception {
        @NotNull TotpTwoFaProviderConfig totpTwoFaProviderConfig = configureTotpTwoFaProvider();

        loginTenantAdmin();

        TotpTwoFaAccountConfig generatedTotpTwoFaAccountConfig = generateTotpTwoFaAccountConfig(totpTwoFaProviderConfig);
        generatedTotpTwoFaAccountConfig.setUseByDefault(true);

        @Nullable String secret = UriComponentsBuilder.fromUriString(generatedTotpTwoFaAccountConfig.getAuthUrl()).build()
                                                      .getQueryParams().getFirst("secret");
        String correctVerificationCode = new Totp(secret).now();

        doPost("/api/2fa/account/config?verificationCode=" + correctVerificationCode, generatedTotpTwoFaAccountConfig)
                .andExpect(status().isOk());

        @NotNull AccountTwoFaSettings accountTwoFaSettings = readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()), AccountTwoFaSettings.class);
        assertThat(accountTwoFaSettings.getConfigs()).size().isOne();

        TwoFaAccountConfig twoFaAccountConfig = accountTwoFaSettings.getConfigs().get(TwoFaProviderType.TOTP);
        assertThat(twoFaAccountConfig).isEqualTo(generatedTotpTwoFaAccountConfig);
    }

    @Test
    public void testVerifyAndSaveTotpTwoFaAccountConfig_incorrectVerificationCode() throws Exception {
        @NotNull TotpTwoFaProviderConfig totpTwoFaProviderConfig = configureTotpTwoFaProvider();

        loginTenantAdmin();

        TotpTwoFaAccountConfig generatedTotpTwoFaAccountConfig = generateTotpTwoFaAccountConfig(totpTwoFaProviderConfig);

        @NotNull String incorrectVerificationCode = "100000";
        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config?verificationCode=" + incorrectVerificationCode, generatedTotpTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));

        assertThat(errorMessage).containsIgnoringCase("verification code is incorrect");
    }

    private TotpTwoFaAccountConfig generateTotpTwoFaAccountConfig(@NotNull TotpTwoFaProviderConfig totpTwoFaProviderConfig) throws Exception {
        @NotNull TwoFaAccountConfig generatedTwoFaAccountConfig = readResponse(doPost("/api/2fa/account/config/generate?providerType=TOTP")
                .andExpect(status().isOk()), TwoFaAccountConfig.class);
        assertThat(generatedTwoFaAccountConfig).isInstanceOf(TotpTwoFaAccountConfig.class);

        assertThat(((TotpTwoFaAccountConfig) generatedTwoFaAccountConfig)).satisfies(accountConfig -> {
            @NotNull UriComponents otpAuthUrl = UriComponentsBuilder.fromUriString(accountConfig.getAuthUrl()).build();
            assertThat(otpAuthUrl.getScheme()).isEqualTo("otpauth");
            assertThat(otpAuthUrl.getHost()).isEqualTo("totp");
            assertThat(otpAuthUrl.getQueryParams().getFirst("issuer")).isEqualTo(totpTwoFaProviderConfig.getIssuerName());
            assertThat(otpAuthUrl.getPath()).isEqualTo("/%s:%s", totpTwoFaProviderConfig.getIssuerName(), TENANT_ADMIN_EMAIL);
            assertThat(otpAuthUrl.getQueryParams().getFirst("secret")).satisfies(secretKey -> {
                assertDoesNotThrow(() -> Base32.decode(secretKey));
            });
        });
        return (TotpTwoFaAccountConfig) generatedTwoFaAccountConfig;
    }

    @Test
    public void testGetTwoFaAccountConfig_whenProviderNotConfigured() throws Exception {
        testVerifyAndSaveTotpTwoFaAccountConfig();
        assertThat(readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()),
                AccountTwoFaSettings.class).getConfigs()).isNotEmpty();

        loginSysAdmin();
        saveProvidersConfigs();

        loginTenantAdmin();

        assertThat(readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()), AccountTwoFaSettings.class).getConfigs())
                .isEmpty();
    }

    @Test
    public void testGenerateSmsTwoFaAccountConfig() throws Exception {
        configureSmsTwoFaProvider("${code}");
        doPost("/api/2fa/account/config/generate?providerType=SMS")
                .andExpect(status().isOk());
    }

    @Test
    public void testSubmitSmsTwoFaAccountConfig() throws Exception {
        @NotNull String verificationMessageTemplate = "Here is your verification code: ${code}";
        configureSmsTwoFaProvider(verificationMessageTemplate);

        loginTenantAdmin();

        @NotNull SmsTwoFaAccountConfig smsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        smsTwoFaAccountConfig.setPhoneNumber("+38054159785");

        doPost("/api/2fa/account/config/submit", smsTwoFaAccountConfig).andExpect(status().isOk());

        String verificationCode = cacheManager.getCache(CacheConstants.TWO_FA_VERIFICATION_CODES_CACHE)
                .get(tenantAdminUserId, OtpBasedTwoFaProvider.Otp.class).getValue();

        verify(smsService).sendSms(eq(tenantId), any(), argThat(phoneNumbers -> {
            return phoneNumbers[0].equals(smsTwoFaAccountConfig.getPhoneNumber());
        }), eq("Here is your verification code: " + verificationCode));
    }

    @Test
    public void testSubmitSmsTwoFaAccountConfig_validationError() throws Exception {
        configureSmsTwoFaProvider("${code}");

        @NotNull SmsTwoFaAccountConfig smsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        @NotNull String blankPhoneNumber = "";
        smsTwoFaAccountConfig.setPhoneNumber(blankPhoneNumber);

        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", smsTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("phone number cannot be blank");

        @NotNull String nonE164PhoneNumber = "8754868";
        smsTwoFaAccountConfig.setPhoneNumber(nonE164PhoneNumber);

        errorMessage = getErrorMessage(doPost("/api/2fa/account/config/submit", smsTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("phone number is not of E.164 format");
    }

    @Test
    public void testVerifyAndSaveSmsTwoFaAccountConfig() throws Exception {
        configureSmsTwoFaProvider("${code}");

        loginTenantAdmin();

        @NotNull SmsTwoFaAccountConfig smsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        smsTwoFaAccountConfig.setPhoneNumber("+38051889445");
        smsTwoFaAccountConfig.setUseByDefault(true);

        @NotNull ArgumentCaptor<String> verificationCodeCaptor = ArgumentCaptor.forClass(String.class);
        doPost("/api/2fa/account/config/submit", smsTwoFaAccountConfig).andExpect(status().isOk());

        verify(smsService).sendSms(eq(tenantId), any(), argThat(phoneNumbers -> {
            return phoneNumbers[0].equals(smsTwoFaAccountConfig.getPhoneNumber());
        }), verificationCodeCaptor.capture());

        String correctVerificationCode = verificationCodeCaptor.getValue();
        doPost("/api/2fa/account/config?verificationCode=" + correctVerificationCode, smsTwoFaAccountConfig)
                .andExpect(status().isOk());

        @NotNull AccountTwoFaSettings accountTwoFaSettings = readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()), AccountTwoFaSettings.class);
        TwoFaAccountConfig accountConfig = accountTwoFaSettings.getConfigs().get(TwoFaProviderType.SMS);
        assertThat(accountConfig).isEqualTo(smsTwoFaAccountConfig);
    }

    @Test
    public void testVerifyAndSaveSmsTwoFaAccountConfig_incorrectVerificationCode() throws Exception {
        configureSmsTwoFaProvider("${code}");

        loginTenantAdmin();

        @NotNull SmsTwoFaAccountConfig smsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        smsTwoFaAccountConfig.setPhoneNumber("+38051889445");

        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config?verificationCode=100000", smsTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("verification code is incorrect");
    }

    @Test
    public void testVerifyAndSaveSmsTwoFaAccountConfig_differentAccountConfigs() throws Exception {
        configureSmsTwoFaProvider("${code}");
        loginTenantAdmin();

        @NotNull SmsTwoFaAccountConfig initialSmsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        initialSmsTwoFaAccountConfig.setPhoneNumber("+38051889445");
        initialSmsTwoFaAccountConfig.setUseByDefault(true);

        @NotNull ArgumentCaptor<String> verificationCodeCaptor = ArgumentCaptor.forClass(String.class);
        doPost("/api/2fa/account/config/submit", initialSmsTwoFaAccountConfig).andExpect(status().isOk());

        verify(smsService).sendSms(eq(tenantId), any(), argThat(phoneNumbers -> {
            return phoneNumbers[0].equals(initialSmsTwoFaAccountConfig.getPhoneNumber());
        }), verificationCodeCaptor.capture());

        String correctVerificationCode = verificationCodeCaptor.getValue();

        @NotNull SmsTwoFaAccountConfig anotherSmsTwoFaAccountConfig = new SmsTwoFaAccountConfig();
        anotherSmsTwoFaAccountConfig.setPhoneNumber("+38111111111");
        String errorMessage = getErrorMessage(doPost("/api/2fa/account/config?verificationCode=" + correctVerificationCode, anotherSmsTwoFaAccountConfig)
                .andExpect(status().isBadRequest()));
        assertThat(errorMessage).containsIgnoringCase("verification code is incorrect");

        doPost("/api/2fa/account/config?verificationCode=" + correctVerificationCode, initialSmsTwoFaAccountConfig)
                .andExpect(status().isOk());
        @NotNull AccountTwoFaSettings accountTwoFaSettings = readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()), AccountTwoFaSettings.class);
        TwoFaAccountConfig accountConfig = accountTwoFaSettings.getConfigs().get(TwoFaProviderType.SMS);
        assertThat(accountConfig).isEqualTo(initialSmsTwoFaAccountConfig);
    }

    @NotNull
    private TotpTwoFaProviderConfig configureTotpTwoFaProvider() throws Exception {
        @NotNull TotpTwoFaProviderConfig totpTwoFaProviderConfig = new TotpTwoFaProviderConfig();
        totpTwoFaProviderConfig.setIssuerName("tb");

        saveProvidersConfigs(totpTwoFaProviderConfig);
        return totpTwoFaProviderConfig;
    }

    @NotNull
    private SmsTwoFaProviderConfig configureSmsTwoFaProvider(String verificationMessageTemplate) throws Exception {
        @NotNull SmsTwoFaProviderConfig smsTwoFaProviderConfig = new SmsTwoFaProviderConfig();
        smsTwoFaProviderConfig.setSmsVerificationMessageTemplate(verificationMessageTemplate);
        smsTwoFaProviderConfig.setVerificationCodeLifetime(60);

        saveProvidersConfigs(smsTwoFaProviderConfig);
        return smsTwoFaProviderConfig;
    }

    private void saveProvidersConfigs(@NotNull TwoFaProviderConfig... providerConfigs) throws Exception {
        @NotNull PlatformTwoFaSettings twoFaSettings = new PlatformTwoFaSettings();
        twoFaSettings.setProviders(Arrays.stream(providerConfigs).collect(Collectors.toList()));
        twoFaSettings.setMinVerificationCodeSendPeriod(5);
        twoFaSettings.setTotalAllowedTimeForVerification(100);
        doPost("/api/2fa/settings", twoFaSettings).andExpect(status().isOk());
    }

    @Test
    public void testIsTwoFaEnabled() throws Exception {
        configureSmsTwoFaProvider("${code}");
        @NotNull SmsTwoFaAccountConfig accountConfig = new SmsTwoFaAccountConfig();
        accountConfig.setPhoneNumber("+38050505050");
        twoFaConfigManager.saveTwoFaAccountConfig(tenantId, tenantAdminUserId, accountConfig);

        assertThat(twoFactorAuthService.isTwoFaEnabled(tenantId, tenantAdminUserId)).isTrue();
    }

    @Test
    public void testDeleteTwoFaAccountConfig() throws Exception {
        configureSmsTwoFaProvider("${code}");
        @NotNull SmsTwoFaAccountConfig accountConfig = new SmsTwoFaAccountConfig();
        accountConfig.setPhoneNumber("+38050505050");

        loginTenantAdmin();

        twoFaConfigManager.saveTwoFaAccountConfig(tenantId, tenantAdminUserId, accountConfig);

        @NotNull AccountTwoFaSettings accountTwoFaSettings = readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()), AccountTwoFaSettings.class);
        TwoFaAccountConfig savedAccountConfig = accountTwoFaSettings.getConfigs().get(TwoFaProviderType.SMS);
        assertThat(savedAccountConfig).isEqualTo(accountConfig);

        doDelete("/api/2fa/account/config?providerType=SMS").andExpect(status().isOk());

        assertThat(readResponse(doGet("/api/2fa/account/settings").andExpect(status().isOk()), AccountTwoFaSettings.class).getConfigs())
                .doesNotContainKey(TwoFaProviderType.SMS);
    }

}
