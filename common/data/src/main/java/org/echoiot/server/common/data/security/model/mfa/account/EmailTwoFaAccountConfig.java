package org.echoiot.server.common.data.security.model.mfa.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.jetbrains.annotations.NotNull;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmailTwoFaAccountConfig extends OtpBasedTwoFaAccountConfig {

    @NotBlank
    @Email
    private String email;

    @NotNull
    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.EMAIL;
    }

}
