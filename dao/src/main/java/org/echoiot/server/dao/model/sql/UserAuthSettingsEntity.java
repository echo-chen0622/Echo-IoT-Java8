package org.echoiot.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.id.UserAuthSettingsId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.UserAuthSettings;
import org.echoiot.server.common.data.security.model.mfa.account.AccountTwoFaSettings;
import org.echoiot.server.dao.model.BaseEntity;
import org.echoiot.server.dao.model.BaseSqlEntity;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.util.mapping.JsonStringType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Entity
@Table(name = ModelConstants.USER_AUTH_SETTINGS_COLUMN_FAMILY_NAME)
public class UserAuthSettingsEntity extends BaseSqlEntity<UserAuthSettings> implements BaseEntity<UserAuthSettings> {

    @Column(name = ModelConstants.USER_AUTH_SETTINGS_USER_ID_PROPERTY, nullable = false, unique = true)
    private UUID userId;
    @Type(type = "json")
    @Column(name = ModelConstants.USER_AUTH_SETTINGS_TWO_FA_SETTINGS)
    private JsonNode twoFaSettings;

    public UserAuthSettingsEntity(@NotNull UserAuthSettings userAuthSettings) {
        if (userAuthSettings.getId() != null) {
            this.setId(userAuthSettings.getId().getId());
        }
        this.setCreatedTime(userAuthSettings.getCreatedTime());
        if (userAuthSettings.getUserId() != null) {
            this.userId = userAuthSettings.getUserId().getId();
        }
        if (userAuthSettings.getTwoFaSettings() != null) {
            this.twoFaSettings = JacksonUtil.valueToTree(userAuthSettings.getTwoFaSettings());
        }
    }

    @NotNull
    @Override
    public UserAuthSettings toData() {
        @NotNull UserAuthSettings userAuthSettings = new UserAuthSettings();
        userAuthSettings.setId(new UserAuthSettingsId(id));
        userAuthSettings.setCreatedTime(createdTime);
        if (userId != null) {
            userAuthSettings.setUserId(new UserId(userId));
        }
        if (twoFaSettings != null) {
            userAuthSettings.setTwoFaSettings(JacksonUtil.treeToValue(twoFaSettings, AccountTwoFaSettings.class));
        }
        return userAuthSettings;
    }

}
