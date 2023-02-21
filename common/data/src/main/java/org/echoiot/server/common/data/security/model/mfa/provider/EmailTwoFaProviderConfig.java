package org.echoiot.server.common.data.security.model.mfa.provider;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmailTwoFaProviderConfig extends OtpBasedTwoFaProviderConfig {

    @NotNull
    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.EMAIL;
    }

}
