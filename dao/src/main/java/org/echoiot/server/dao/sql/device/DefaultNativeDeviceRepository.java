package org.echoiot.server.dao.sql.device;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DeviceIdInfo;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
@Slf4j
public class DefaultNativeDeviceRepository implements NativeDeviceRepository {

    private final String COUNT_QUERY = "SELECT count(id) FROM device;";
    private final String QUERY = "SELECT tenant_id as tenantId, customer_id as customerId, id as id FROM device ORDER BY created_time ASC LIMIT %s OFFSET %s";
    @NotNull
    private final NamedParameterJdbcTemplate jdbcTemplate;
    @NotNull
    private final TransactionTemplate transactionTemplate;

    @Nullable
    @Override
    public PageData<DeviceIdInfo> findDeviceIdInfos(@NotNull Pageable pageable) {
        return transactionTemplate.execute(status -> {
            long startTs = System.currentTimeMillis();
            int totalElements = jdbcTemplate.queryForObject(COUNT_QUERY, Collections.emptyMap(), Integer.class);
            log.debug("Count query took {} ms", System.currentTimeMillis() - startTs);
            startTs = System.currentTimeMillis();
            @NotNull List<Map<String, Object>> rows = jdbcTemplate.queryForList(String.format(QUERY, pageable.getPageSize(), pageable.getOffset()), Collections.emptyMap());
            log.debug("Main query took {} ms", System.currentTimeMillis() - startTs);
            int totalPages = pageable.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageable.getPageSize()) : 1;
            boolean hasNext = pageable.getPageSize() > 0 && totalElements > pageable.getOffset() + rows.size();
            @NotNull var data = rows.stream().map(row -> {
                UUID id = (UUID) row.get("id");
                var tenantIdObj = row.get("tenantId");
                var customerIdObj = row.get("customerId");
                return new DeviceIdInfo(tenantIdObj != null ? (UUID) tenantIdObj : TenantId.SYS_TENANT_ID.getId(), customerIdObj != null ? (UUID) customerIdObj : null, id);
            }).collect(Collectors.toList());
            return new PageData<>(data, totalPages, totalElements, hasNext);
        });
    }
}
