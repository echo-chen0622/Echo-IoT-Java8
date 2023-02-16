package org.echoiot.server.common.data.security.model.mfa.account;

import lombok.Data;
import org.echoiot.server.common.data.security.model.mfa.provider.TwoFaProviderType;

import java.util.LinkedHashMap;

@Data
public class AccountTwoFaSettings {
    private LinkedHashMap<TwoFaProviderType, TwoFaAccountConfig> configs;
}
