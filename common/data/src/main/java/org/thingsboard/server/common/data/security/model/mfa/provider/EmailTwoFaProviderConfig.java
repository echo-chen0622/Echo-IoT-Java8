package org.thingsboard.server.common.data.security.model.mfa.provider;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmailTwoFaProviderConfig extends OtpBasedTwoFaProviderConfig {

    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.EMAIL;
    }

}
