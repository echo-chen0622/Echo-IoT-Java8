package org.thingsboard.server.dao.sql.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.UserCredentialsEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.user.UserCredentialsDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/22/2017.
 */
@Component
@SqlDao
public class JpaUserCredentialsDao extends JpaAbstractDao<UserCredentialsEntity, UserCredentials> implements UserCredentialsDao {

    @Autowired
    private UserCredentialsRepository userCredentialsRepository;

    @Override
    protected Class<UserCredentialsEntity> getEntityClass() {
        return UserCredentialsEntity.class;
    }

    @Override
    protected JpaRepository<UserCredentialsEntity, UUID> getRepository() {
        return userCredentialsRepository;
    }

    @Override
    public UserCredentials findByUserId(TenantId tenantId, UUID userId) {
        return DaoUtil.getData(userCredentialsRepository.findByUserId(userId));
    }

    @Override
    public UserCredentials findByActivateToken(TenantId tenantId, String activateToken) {
        return DaoUtil.getData(userCredentialsRepository.findByActivateToken(activateToken));
    }

    @Override
    public UserCredentials findByResetToken(TenantId tenantId, String resetToken) {
        return DaoUtil.getData(userCredentialsRepository.findByResetToken(resetToken));
    }
}
