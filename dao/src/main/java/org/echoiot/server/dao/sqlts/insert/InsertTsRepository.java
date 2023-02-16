package org.echoiot.server.dao.sqlts.insert;

import org.echoiot.server.dao.model.sql.AbstractTsKvEntity;

import java.util.List;

public interface InsertTsRepository<T extends AbstractTsKvEntity> {

    void saveOrUpdate(List<T> entities);

}
