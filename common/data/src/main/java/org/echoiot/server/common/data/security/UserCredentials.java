package org.echoiot.server.common.data.security;

import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.BaseData;
import org.echoiot.server.common.data.id.UserCredentialsId;
import org.echoiot.server.common.data.id.UserId;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
public class UserCredentials extends BaseData<UserCredentialsId> {

    private static final long serialVersionUID = -2108436378880529163L;

    private UserId userId;
    private boolean enabled;
    private String password;
    private String activateToken;
    private String resetToken;

    public UserCredentials() {
        super();
    }

    public UserCredentials(UserCredentialsId id) {
        super(id);
    }

    public UserCredentials(@NotNull UserCredentials userCredentials) {
        super(userCredentials);
        this.userId = userCredentials.getUserId();
        this.password = userCredentials.getPassword();
        this.enabled = userCredentials.isEnabled();
        this.activateToken = userCredentials.getActivateToken();
        this.resetToken = userCredentials.getResetToken();
    }

    public UserId getUserId() {
        return userId;
    }

    public void setUserId(UserId userId) {
        this.userId = userId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getActivateToken() {
        return activateToken;
    }

    public void setActivateToken(String activateToken) {
        this.activateToken = activateToken;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    @NotNull
    @Override
    public String toString() {
        String builder = "UserCredentials [userId=" +
                         userId +
                         ", enabled=" +
                         enabled +
                         ", password=" +
                         password +
                         ", activateToken=" +
                         activateToken +
                         ", resetToken=" +
                         resetToken +
                         ", createdTime=" +
                         createdTime +
                         ", id=" +
                         id +
                         "]";
        return builder;
    }

}
