package org.thingsboard.server.common.data.security.model.mfa.provider;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
public class SmsTwoFaProviderConfig extends OtpBasedTwoFaProviderConfig {

    @NotBlank(message = "verification message template is required")
    @Pattern(regexp = ".*\\$\\{code}.*", message = "template must contain verification code")
    private String smsVerificationMessageTemplate;

    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.SMS;
    }

}
