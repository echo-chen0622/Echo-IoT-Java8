package org.thingsboard.server.common.data.security.model.mfa.provider;

import lombok.Data;

import javax.validation.constraints.Min;

@Data
public abstract class OtpBasedTwoFaProviderConfig implements TwoFaProviderConfig {

    @Min(value = 1, message = "verification code lifetime is required")
    private int verificationCodeLifetime;

}
