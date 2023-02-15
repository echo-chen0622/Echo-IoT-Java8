package org.thingsboard.server.dao.sql.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.UserAuthSettingsEntity;

import java.util.UUID;

@Repository
public interface UserAuthSettingsRepository extends JpaRepository<UserAuthSettingsEntity, UUID> {

    UserAuthSettingsEntity findByUserId(UUID userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserAuthSettingsEntity e WHERE e.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

}
