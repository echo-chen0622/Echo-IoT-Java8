package org.thingsboard.server.common.data.security.model.mfa.provider;

import lombok.Data;

import javax.validation.constraints.Min;

@Data
public class BackupCodeTwoFaProviderConfig implements TwoFaProviderConfig {

    @Min(value = 1, message = "backup codes quantity must be greater than 0")
    private int codesQuantity;

    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.BACKUP_CODE;
    }

}
