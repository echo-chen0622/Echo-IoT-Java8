package org.thingsboard.server.common.data.security.model.mfa.provider;

public enum TwoFaProviderType {
    TOTP,
    SMS,
    EMAIL,
    BACKUP_CODE
}
