package org.thingsboard.server.dao.sql.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.oauth2.OAuth2Domain;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.OAuth2DomainEntity;
import org.thingsboard.server.dao.oauth2.OAuth2DomainDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@SqlDao
public class JpaOAuth2DomainDao extends JpaAbstractDao<OAuth2DomainEntity, OAuth2Domain> implements OAuth2DomainDao {

    private final OAuth2DomainRepository repository;

    @Override
    protected Class<OAuth2DomainEntity> getEntityClass() {
        return OAuth2DomainEntity.class;
    }

    @Override
    protected JpaRepository<OAuth2DomainEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public List<OAuth2Domain> findByOAuth2ParamsId(UUID oauth2ParamsId) {
        return DaoUtil.convertDataList(repository.findByOauth2ParamsId(oauth2ParamsId));
    }

}
