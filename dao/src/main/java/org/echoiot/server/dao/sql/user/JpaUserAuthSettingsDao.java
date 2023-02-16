package org.echoiot.server.dao.sql.user;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.dao.model.sql.UserAuthSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.security.UserAuthSettings;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.sql.JpaAbstractDao;
import org.echoiot.server.dao.user.UserAuthSettingsDao;
import org.echoiot.server.dao.util.SqlDao;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@SqlDao
public class JpaUserAuthSettingsDao extends JpaAbstractDao<UserAuthSettingsEntity, UserAuthSettings> implements UserAuthSettingsDao {

    private final UserAuthSettingsRepository repository;

    @Override
    public UserAuthSettings findByUserId(UserId userId) {
        return DaoUtil.getData(repository.findByUserId(userId.getId()));
    }

    @Override
    public void removeByUserId(UserId userId) {
        repository.deleteByUserId(userId.getId());
    }

    @Override
    protected Class<UserAuthSettingsEntity> getEntityClass() {
        return UserAuthSettingsEntity.class;
    }

    @Override
    protected JpaRepository<UserAuthSettingsEntity, UUID> getRepository() {
        return repository;
    }

}