package org.thingsboard.server.common.data.security.model.mfa.account;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class OtpBasedTwoFaAccountConfig extends TwoFaAccountConfig {
}
