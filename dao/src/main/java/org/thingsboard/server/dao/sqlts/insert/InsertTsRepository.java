package org.thingsboard.server.dao.sqlts.insert;

import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;

import java.util.List;

public interface InsertTsRepository<T extends AbstractTsKvEntity> {

    void saveOrUpdate(List<T> entities);

}
