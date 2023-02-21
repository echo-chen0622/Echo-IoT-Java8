package org.echoiot.server.dao;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface Dao<T> {

    List<T> find(TenantId tenantId);

    T findById(TenantId tenantId, UUID id);

    ListenableFuture<T> findByIdAsync(TenantId tenantId, UUID id);

    boolean existsById(TenantId tenantId, UUID id);

    ListenableFuture<Boolean> existsByIdAsync(TenantId tenantId, UUID id);

    T save(TenantId tenantId, T t);

    T saveAndFlush(TenantId tenantId, T t);

    boolean removeById(TenantId tenantId, UUID id);

    void removeAllByIds(Collection<UUID> ids);

    @Nullable
    default EntityType getEntityType() { return null; }

}
