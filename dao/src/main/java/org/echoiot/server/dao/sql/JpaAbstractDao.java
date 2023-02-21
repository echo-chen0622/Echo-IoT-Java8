package org.echoiot.server.dao.sql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.Dao;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.model.BaseEntity;
import org.echoiot.server.dao.util.SqlDao;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Valerii Sosliuk
 */
@Slf4j
@SqlDao
public abstract class JpaAbstractDao<E extends BaseEntity<D>, D>
        extends JpaAbstractDaoListeningExecutorService
        implements Dao<D> {

    protected abstract Class<E> getEntityClass();

    protected abstract JpaRepository<E, UUID> getRepository();

    protected void setSearchText(E entity) {
    }

    @Override
    @Transactional
    public D save(TenantId tenantId, @NotNull D domain) {
        E entity;
        try {
            entity = getEntityClass().getConstructor(domain.getClass()).newInstance(domain);
        } catch (Exception e) {
            log.error("Can't create entity for domain object {}", domain, e);
            throw new IllegalArgumentException("Can't create entity for domain object {" + domain + "}", e);
        }
        setSearchText(entity);
        log.debug("Saving entity {}", entity);
        if (entity.getUuid() == null) {
            @NotNull UUID uuid = Uuids.timeBased();
            entity.setUuid(uuid);
            entity.setCreatedTime(Uuids.unixTimestamp(uuid));
        }
        entity = getRepository().save(entity);
        return DaoUtil.getData(entity);
    }

    @Override
    @Transactional
    public D saveAndFlush(TenantId tenantId, @NotNull D domain) {
        D d = save(tenantId, domain);
        getRepository().flush();
        return d;
    }

    @Override
    public D findById(TenantId tenantId, @NotNull UUID key) {
        log.debug("Get entity by key {}", key);
        @NotNull Optional<E> entity = getRepository().findById(key);
        return DaoUtil.getData(entity);
    }

    @Override
    public ListenableFuture<D> findByIdAsync(TenantId tenantId, @NotNull UUID key) {
        log.debug("Get entity by key async {}", key);
        return service.submit(() -> DaoUtil.getData(getRepository().findById(key)));
    }

    @Override
    public boolean existsById(TenantId tenantId, @NotNull UUID key) {
        log.debug("Exists by key {}", key);
        return getRepository().existsById(key);
    }

    @Override
    public ListenableFuture<Boolean> existsByIdAsync(TenantId tenantId, @NotNull UUID key) {
        log.debug("Exists by key async {}", key);
        return service.submit(() -> getRepository().existsById(key));
    }

    @Override
    @Transactional
    public boolean removeById(TenantId tenantId, @NotNull UUID id) {
        getRepository().deleteById(id);
        log.debug("Remove request: {}", id);
        return !getRepository().existsById(id);
    }

    @Transactional
    public void removeAllByIds(@NotNull Collection<UUID> ids) {
        JpaRepository<E, UUID> repository = getRepository();
        ids.forEach(repository::deleteById);
    }

    @Override
    public List<D> find(TenantId tenantId) {
        @NotNull List<E> entities = Lists.newArrayList(getRepository().findAll());
        return DaoUtil.convertDataList(entities);
    }
}
