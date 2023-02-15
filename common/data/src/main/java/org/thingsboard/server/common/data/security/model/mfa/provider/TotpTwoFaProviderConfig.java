package org.thingsboard.server.common.data.security.model.mfa.provider;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TotpTwoFaProviderConfig implements TwoFaProviderConfig {

    @NotBlank(message = "issuer name must not be blank")
    private String issuerName;

    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.TOTP;
    }

}
