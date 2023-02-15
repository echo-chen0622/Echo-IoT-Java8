package org.thingsboard.server.dao.sqlts.latest;

import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.util.SqlTsLatestAnyDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

@SqlTsLatestAnyDao
@Repository
public class SearchTsKvLatestRepository {

    public static final String FIND_ALL_BY_ENTITY_ID = "findAllByEntityId";

    public static final String FIND_ALL_BY_ENTITY_ID_QUERY = "SELECT ts_kv_latest.entity_id AS entityId, ts_kv_latest.key AS key, ts_kv_dictionary.key AS strKey, ts_kv_latest.str_v AS strValue," +
            " ts_kv_latest.bool_v AS boolValue, ts_kv_latest.long_v AS longValue, ts_kv_latest.dbl_v AS doubleValue, ts_kv_latest.json_v AS jsonValue, ts_kv_latest.ts AS ts FROM ts_kv_latest " +
            "INNER JOIN ts_kv_dictionary ON ts_kv_latest.key = ts_kv_dictionary.key_id WHERE ts_kv_latest.entity_id = cast(:id AS uuid)";

    @PersistenceContext
    private EntityManager entityManager;

    public List<TsKvLatestEntity> findAllByEntityId(UUID entityId) {
        return entityManager.createNamedQuery(FIND_ALL_BY_ENTITY_ID, TsKvLatestEntity.class)
                .setParameter("id", entityId)
                .getResultList();
    }

}
