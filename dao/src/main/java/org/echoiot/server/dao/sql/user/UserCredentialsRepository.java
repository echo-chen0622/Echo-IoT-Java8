package org.echoiot.server.dao.sql.user;

import org.echoiot.server.dao.model.sql.UserCredentialsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/22/2017.
 */
public interface UserCredentialsRepository extends JpaRepository<UserCredentialsEntity, UUID> {

    UserCredentialsEntity findByUserId(UUID userId);

    UserCredentialsEntity findByActivateToken(String activateToken);

    UserCredentialsEntity findByResetToken(String resetToken);
}
