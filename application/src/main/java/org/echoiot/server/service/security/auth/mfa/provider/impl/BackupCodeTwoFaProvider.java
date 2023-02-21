package org.echoiot.server.service.security.auth.mfa.provider.impl;

import org.echoiot.common.util.CollectionsUtil;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.security.model.mfa.account.BackupCodeTwoFaAccountConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.BackupCodeTwoFaProviderConfig;
import org.echoiot.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.security.auth.mfa.config.TwoFaConfigManager;
import org.echoiot.server.service.security.auth.mfa.provider.TwoFaProvider;
import org.echoiot.server.service.security.model.SecurityUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@TbCoreComponent
public class BackupCodeTwoFaProvider implements TwoFaProvider<BackupCodeTwoFaProviderConfig, BackupCodeTwoFaAccountConfig> {

    @Resource @Lazy
    private TwoFaConfigManager twoFaConfigManager;

    @NotNull
    @Override
    public BackupCodeTwoFaAccountConfig generateNewAccountConfig(User user, @NotNull BackupCodeTwoFaProviderConfig providerConfig) {
        @NotNull BackupCodeTwoFaAccountConfig config = new BackupCodeTwoFaAccountConfig();
        config.setCodes(generateCodes(providerConfig.getCodesQuantity(), 8));
        config.setSerializeHiddenFields(true);
        return config;
    }

    @NotNull
    private static Set<String> generateCodes(int count, int length) {
        return Stream.generate(() -> StringUtils.random(length, "0123456789abcdef"))
                .distinct().limit(count)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean checkVerificationCode(@NotNull SecurityUser user, String code, BackupCodeTwoFaProviderConfig providerConfig, @NotNull BackupCodeTwoFaAccountConfig accountConfig) {
        if (CollectionsUtil.contains(accountConfig.getCodes(), code)) {
            accountConfig.getCodes().remove(code);
            twoFaConfigManager.saveTwoFaAccountConfig(user.getTenantId(), user.getId(), accountConfig);
            return true;
        } else {
            return false;
        }
    }

    @NotNull
    @Override
    public TwoFaProviderType getType() {
        return TwoFaProviderType.BACKUP_CODE;
    }

}
