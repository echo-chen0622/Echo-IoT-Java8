package org.thingsboard.server.dao.sql.relation;

import org.thingsboard.server.dao.model.sql.RelationEntity;

import java.util.List;

public interface RelationInsertRepository {

    RelationEntity saveOrUpdate(RelationEntity entity);

    void saveOrUpdate(List<RelationEntity> entities);

}
