package org.echoiot.server.common.data.security.model.mfa.provider;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import javax.validation.constraints.NotBlank;

@Data
public class TotpTwoFaProviderConfig implements TwoFaProviderConfig {

    @NotBlank(message = "issuer name must not be blank")
    private String issuerName;

    @NotNull
    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.TOTP;
    }

}
