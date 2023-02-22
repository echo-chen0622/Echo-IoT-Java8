package org.echoiot.server.dao.sql.oauth2;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.oauth2.OAuth2Params;
import org.echoiot.server.dao.model.sql.OAuth2ParamsEntity;
import org.echoiot.server.dao.oauth2.OAuth2ParamsDao;
import org.echoiot.server.dao.sql.JpaAbstractDao;
import org.echoiot.server.dao.util.SqlDao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@SqlDao
public class JpaOAuth2ParamsDao extends JpaAbstractDao<OAuth2ParamsEntity, OAuth2Params> implements OAuth2ParamsDao {
    private final OAuth2ParamsRepository repository;

    @Override
    protected Class<OAuth2ParamsEntity> getEntityClass() {
        return OAuth2ParamsEntity.class;
    }

    @Override
    protected JpaRepository<OAuth2ParamsEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }
}
