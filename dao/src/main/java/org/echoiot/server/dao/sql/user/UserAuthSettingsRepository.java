package org.echoiot.server.dao.sql.user;

import org.echoiot.server.dao.model.sql.UserAuthSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public interface UserAuthSettingsRepository extends JpaRepository<UserAuthSettingsEntity, UUID> {

    UserAuthSettingsEntity findByUserId(UUID userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserAuthSettingsEntity e WHERE e.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

}
