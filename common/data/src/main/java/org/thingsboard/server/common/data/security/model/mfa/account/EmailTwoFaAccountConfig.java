package org.thingsboard.server.common.data.security.model.mfa.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmailTwoFaAccountConfig extends OtpBasedTwoFaAccountConfig {

    @NotBlank
    @Email
    private String email;

    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.EMAIL;
    }

}
