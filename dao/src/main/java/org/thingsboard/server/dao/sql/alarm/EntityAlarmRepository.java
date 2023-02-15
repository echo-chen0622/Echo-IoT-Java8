package org.thingsboard.server.dao.sql.alarm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.EntityAlarmCompositeKey;
import org.thingsboard.server.dao.model.sql.EntityAlarmEntity;

import java.util.List;
import java.util.UUID;

public interface EntityAlarmRepository extends JpaRepository<EntityAlarmEntity, EntityAlarmCompositeKey> {

    List<EntityAlarmEntity> findAllByAlarmId(UUID alarmId);

    @Transactional
    @Modifying
    @Query("DELETE FROM EntityAlarmEntity e where e.entityId = :entityId")
    void deleteByEntityId(@Param("entityId") UUID entityId);
}
