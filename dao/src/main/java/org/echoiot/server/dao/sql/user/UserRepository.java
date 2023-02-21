package org.echoiot.server.dao.sql.user;

import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.dao.model.sql.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * @author Valerii Sosliuk
 */
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    UserEntity findByEmail(String email);

    UserEntity findByTenantIdAndEmail(UUID tenantId, String email);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND u.customerId = :customerId AND u.authority = :authority " +
            "AND LOWER(u.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<UserEntity> findUsersByAuthority(@Param("tenantId") UUID tenantId,
                                          @Param("customerId") UUID customerId,
                                          @Param("searchText") String searchText,
                                          @Param("authority") Authority authority,
                                          Pageable pageable);

    @Query("SELECT u FROM UserEntity u WHERE u.tenantId = :tenantId " +
            "AND LOWER(u.searchText) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<UserEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                    @Param("searchText") String searchText,
                                    Pageable pageable);

    Long countByTenantId(UUID tenantId);

}
