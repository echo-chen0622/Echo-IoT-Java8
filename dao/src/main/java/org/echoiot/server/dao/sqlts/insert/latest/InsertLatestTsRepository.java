package org.echoiot.server.dao.sqlts.insert.latest;

import org.echoiot.server.dao.model.sqlts.latest.TsKvLatestEntity;

import java.util.List;

public interface InsertLatestTsRepository {

    void saveOrUpdate(List<TsKvLatestEntity> entities);

}
