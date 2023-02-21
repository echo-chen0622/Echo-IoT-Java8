package org.echoiot.server.common.data.security.model.mfa.account;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.NotEmpty;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class BackupCodeTwoFaAccountConfig extends TwoFaAccountConfig {

    @NotEmpty
    private Set<String> codes;

    @NotNull
    @Override
    public TwoFaProviderType getProviderType() {
        return TwoFaProviderType.BACKUP_CODE;
    }


    @Nullable
    @JsonGetter("codes")
    private Set<String> getCodesForJson() {
        if (serializeHiddenFields) {
            return codes;
        } else {
            return null;
        }
    }

    @Nullable
    @JsonGetter
    private Integer getCodesLeft() {
        if (codes != null) {
            return codes.size();
        } else {
            return null;
        }
    }

}
