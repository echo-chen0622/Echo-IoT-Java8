package org.thingsboard.server.dao.sqlts.insert.latest;

import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;

import java.util.List;

public interface InsertLatestTsRepository {

    void saveOrUpdate(List<TsKvLatestEntity> entities);

}
