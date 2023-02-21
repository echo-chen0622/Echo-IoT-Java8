package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.id.UserCredentialsId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.UserCredentials;
import org.echoiot.server.dao.model.BaseEntity;
import org.echoiot.server.dao.model.BaseSqlEntity;
import org.echoiot.server.dao.model.ModelConstants;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.USER_CREDENTIALS_COLUMN_FAMILY_NAME)
public final class UserCredentialsEntity extends BaseSqlEntity<UserCredentials> implements BaseEntity<UserCredentials> {

    @Column(name = ModelConstants.USER_CREDENTIALS_USER_ID_PROPERTY, unique = true)
    private UUID userId;

    @Column(name = ModelConstants.USER_CREDENTIALS_ENABLED_PROPERTY)
    private boolean enabled;

    @Column(name = ModelConstants.USER_CREDENTIALS_PASSWORD_PROPERTY)
    private String password;

    @Column(name = ModelConstants.USER_CREDENTIALS_ACTIVATE_TOKEN_PROPERTY, unique = true)
    private String activateToken;

    @Column(name = ModelConstants.USER_CREDENTIALS_RESET_TOKEN_PROPERTY, unique = true)
    private String resetToken;

    public UserCredentialsEntity() {
        super();
    }

    public UserCredentialsEntity(@NotNull UserCredentials userCredentials) {
        if (userCredentials.getId() != null) {
            this.setUuid(userCredentials.getId().getId());
        }
        this.setCreatedTime(userCredentials.getCreatedTime());
        if (userCredentials.getUserId() != null) {
            this.userId = userCredentials.getUserId().getId();
        }
        this.enabled = userCredentials.isEnabled();
        this.password = userCredentials.getPassword();
        this.activateToken = userCredentials.getActivateToken();
        this.resetToken = userCredentials.getResetToken();
    }

    @NotNull
    @Override
    public UserCredentials toData() {
        @NotNull UserCredentials userCredentials = new UserCredentials(new UserCredentialsId(this.getUuid()));
        userCredentials.setCreatedTime(createdTime);
        if (userId != null) {
            userCredentials.setUserId(new UserId(userId));
        }
        userCredentials.setEnabled(enabled);
        userCredentials.setPassword(password);
        userCredentials.setActivateToken(activateToken);
        userCredentials.setResetToken(resetToken);
        return userCredentials;
    }

}
