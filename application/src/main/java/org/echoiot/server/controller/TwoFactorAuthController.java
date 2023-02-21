package org.echoiot.server.controller;

import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.security.model.JwtPair;
import org.echoiot.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.echoiot.server.common.data.security.model.mfa.account.EmailTwoFaAccountConfig;
import org.echoiot.server.common.data.security.model.mfa.account.SmsTwoFaAccountConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.echoiot.server.dao.user.UserService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.security.auth.mfa.TwoFactorAuthService;
import org.echoiot.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.echoiot.server.service.security.auth.rest.RestAuthenticationDetails;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.token.JwtTokenFactory;
import org.echoiot.server.service.security.system.SystemSecurityService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.echoiot.server.controller.ControllerConstants.NEW_LINE;

@RestController
@RequestMapping("/api/auth/2fa")
@TbCoreComponent
@RequiredArgsConstructor
public class TwoFactorAuthController extends BaseController {

    @NotNull
    private final TwoFactorAuthService twoFactorAuthService;
    @NotNull
    private final TwoFaConfigManager twoFaConfigManager;
    @NotNull
    private final JwtTokenFactory tokenFactory;
    @NotNull
    private final SystemSecurityService systemSecurityService;
    @NotNull
    private final UserService userService;


    @ApiOperation(value = "Request 2FA verification code (requestTwoFaVerificationCode)",
            notes = "Request 2FA verification code." + NEW_LINE +
                    "To make a request to this endpoint, you need an access token with the scope of PRE_VERIFICATION_TOKEN, " +
                    "which is issued on username/password auth if 2FA is enabled." + NEW_LINE +
                    "The API method is rate limited (using rate limit config from TwoFactorAuthSettings). " +
                    "Will return a Bad Request error if provider is not configured for usage, " +
                    "and Too Many Requests error if rate limits are exceeded.")
    @PostMapping("/verification/send")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    public void requestTwoFaVerificationCode(@RequestParam TwoFaProviderType providerType) throws Exception {
        SecurityUser user = getCurrentUser();
        twoFactorAuthService.prepareVerificationCode(user, providerType, true);
    }

    @ApiOperation(value = "Check 2FA verification code (checkTwoFaVerificationCode)",
            notes = "Checks 2FA verification code, and if it is correct the method returns a regular access and refresh token pair." + NEW_LINE +
                    "The API method is rate limited (using rate limit config from TwoFactorAuthSettings), and also will block a user " +
                    "after X unsuccessful verification attempts if such behavior is configured (in TwoFactorAuthSettings)." + NEW_LINE +
                    "Will return a Bad Request error if provider is not configured for usage, " +
                    "and Too Many Requests error if rate limits are exceeded.")
    @PostMapping("/verification/check")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    public JwtPair checkTwoFaVerificationCode(@RequestParam TwoFaProviderType providerType,
                                              @RequestParam String verificationCode, @NotNull HttpServletRequest servletRequest) throws Exception {
        SecurityUser user = getCurrentUser();
        boolean verificationSuccess = twoFactorAuthService.checkVerificationCode(user, providerType, verificationCode, true);
        if (verificationSuccess) {
            systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(servletRequest), ActionType.LOGIN, null);
            user = new SecurityUser(userService.findUserById(user.getTenantId(), user.getId()), true, user.getUserPrincipal());
            return tokenFactory.createTokenPair(user);
        } else {
            @NotNull EchoiotException error = new EchoiotException("Verification code is incorrect", EchoiotErrorCode.BAD_REQUEST_PARAMS);
            systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(servletRequest), ActionType.LOGIN, error);
            throw error;
        }
    }


    @NotNull
    @ApiOperation(value = "Get available 2FA providers (getAvailableTwoFaProviders)", notes =
            "Get the list of 2FA provider infos available for user to use. Example:\n" +
                    "```\n[\n" +
                    "  {\n    \"type\": \"EMAIL\",\n    \"default\": true,\n    \"contact\": \"ab*****ko@gmail.com\"\n  },\n" +
                    "  {\n    \"type\": \"TOTP\",\n    \"default\": false,\n    \"contact\": null\n  },\n" +
                    "  {\n    \"type\": \"SMS\",\n    \"default\": false,\n    \"contact\": \"+38********12\"\n  }\n" +
                    "]\n```")
    @GetMapping("/providers")
    @PreAuthorize("hasAuthority('PRE_VERIFICATION_TOKEN')")
    public List<TwoFaProviderInfo> getAvailableTwoFaProviders() throws EchoiotException {
        SecurityUser user = getCurrentUser();
        Optional<PlatformTwoFaSettings> platformTwoFaSettings = twoFaConfigManager.getPlatformTwoFaSettings(user.getTenantId(), true);
        return twoFaConfigManager.getAccountTwoFaSettings(user.getTenantId(), user.getId())
                .map(settings -> settings.getConfigs().values()).orElse(Collections.emptyList())
                .stream().map(config -> {
                    @Nullable String contact = null;
                    switch (config.getProviderType()) {
                        case SMS:
                            String phoneNumber = ((SmsTwoFaAccountConfig) config).getPhoneNumber();
                            contact = StringUtils.obfuscate(phoneNumber, 2, '*', phoneNumber.indexOf('+') + 1, phoneNumber.length());
                            break;
                        case EMAIL:
                            String email = ((EmailTwoFaAccountConfig) config).getEmail();
                            contact = StringUtils.obfuscate(email, 2, '*', 0, email.indexOf('@'));
                            break;
                    }
                    return TwoFaProviderInfo.builder()
                            .type(config.getProviderType())
                            .isDefault(config.isUseByDefault())
                            .contact(contact)
                            .minVerificationCodeSendPeriod(platformTwoFaSettings.get().getMinVerificationCodeSendPeriod())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class TwoFaProviderInfo {
        private TwoFaProviderType type;
        private boolean isDefault;
        private String contact;
        private Integer minVerificationCodeSendPeriod;
    }

}
