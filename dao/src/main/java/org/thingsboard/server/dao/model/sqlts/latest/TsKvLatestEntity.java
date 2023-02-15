package org.thingsboard.server.dao.model.sqlts.latest;

import lombok.Data;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;
import org.thingsboard.server.dao.sqlts.latest.SearchTsKvLatestRepository;

import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import java.util.UUID;

@Data
@Entity
@Table(name = "ts_kv_latest")
@IdClass(TsKvLatestCompositeKey.class)
@SqlResultSetMappings({
        @SqlResultSetMapping(
                name = "tsKvLatestFindMapping",
                classes = {
                        @ConstructorResult(
                                targetClass = TsKvLatestEntity.class,
                                columns = {
                                        @ColumnResult(name = "entityId", type = UUID.class),
                                        @ColumnResult(name = "key", type = Integer.class),
                                        @ColumnResult(name = "strKey", type = String.class),
                                        @ColumnResult(name = "strValue", type = String.class),
                                        @ColumnResult(name = "boolValue", type = Boolean.class),
                                        @ColumnResult(name = "longValue", type = Long.class),
                                        @ColumnResult(name = "doubleValue", type = Double.class),
                                        @ColumnResult(name = "jsonValue", type = String.class),
                                        @ColumnResult(name = "ts", type = Long.class),

                                }
                        ),
                })
})
@NamedNativeQueries({
        @NamedNativeQuery(
                name = SearchTsKvLatestRepository.FIND_ALL_BY_ENTITY_ID,
                query = SearchTsKvLatestRepository.FIND_ALL_BY_ENTITY_ID_QUERY,
                resultSetMapping = "tsKvLatestFindMapping",
                resultClass = TsKvLatestEntity.class
        )
})
public final class TsKvLatestEntity extends AbstractTsKvEntity {

    @Override
    public boolean isNotEmpty() {
        return strValue != null || longValue != null || doubleValue != null || booleanValue != null || jsonValue != null;
    }

    public TsKvLatestEntity() {
    }

    public TsKvLatestEntity(UUID entityId, Integer key, String strKey, String strValue, Boolean boolValue, Long longValue, Double doubleValue, String jsonValue, Long ts) {
        this.entityId = entityId;
        this.key = key;
        this.ts = ts;
        this.longValue = longValue;
        this.doubleValue = doubleValue;
        this.strValue = strValue;
        this.booleanValue = boolValue;
        this.jsonValue = jsonValue;
        this.strKey = strKey;
    }
}
