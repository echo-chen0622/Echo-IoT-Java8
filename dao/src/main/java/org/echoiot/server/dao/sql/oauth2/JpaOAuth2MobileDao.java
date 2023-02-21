package org.echoiot.server.dao.sql.oauth2;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.oauth2.OAuth2Mobile;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.model.sql.OAuth2MobileEntity;
import org.echoiot.server.dao.oauth2.OAuth2MobileDao;
import org.echoiot.server.dao.sql.JpaAbstractDao;
import org.echoiot.server.dao.util.SqlDao;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@SqlDao
public class JpaOAuth2MobileDao extends JpaAbstractDao<OAuth2MobileEntity, OAuth2Mobile> implements OAuth2MobileDao {

    @NotNull
    private final OAuth2MobileRepository repository;

    @NotNull
    @Override
    protected Class<OAuth2MobileEntity> getEntityClass() {
        return OAuth2MobileEntity.class;
    }

    @Override
    protected JpaRepository<OAuth2MobileEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public List<OAuth2Mobile> findByOAuth2ParamsId(UUID oauth2ParamsId) {
        return DaoUtil.convertDataList(repository.findByOauth2ParamsId(oauth2ParamsId));
    }

}
