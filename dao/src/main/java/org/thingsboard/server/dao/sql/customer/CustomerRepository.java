package org.thingsboard.server.dao.sql.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.CustomerEntity;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID>, ExportableEntityRepository<CustomerEntity> {

    @Query("SELECT c FROM CustomerEntity c WHERE c.tenantId = :tenantId " +
            "AND LOWER(c.searchText) LIKE LOWER(CONCAT('%', :textSearch, '%'))")
    Page<CustomerEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                        @Param("textSearch") String textSearch,
                                        Pageable pageable);

    CustomerEntity findByTenantIdAndTitle(UUID tenantId, String title);

    Long countByTenantId(UUID tenantId);

    @Query("SELECT externalId FROM CustomerEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}
