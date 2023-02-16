package org.echoiot.server.dao.sql.relation;

import org.echoiot.server.dao.model.sql.RelationEntity;

import java.util.List;

public interface RelationInsertRepository {

    RelationEntity saveOrUpdate(RelationEntity entity);

    void saveOrUpdate(List<RelationEntity> entities);

}
