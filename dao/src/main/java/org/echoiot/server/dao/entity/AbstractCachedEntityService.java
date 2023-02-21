package org.echoiot.server.dao.entity;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.echoiot.server.cache.TbTransactionalCache;

import java.io.Serializable;

public abstract class AbstractCachedEntityService<K extends Serializable, V extends Serializable, E> extends AbstractEntityService {

    @Resource
    protected TbTransactionalCache<K, V> cache;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    protected void publishEvictEvent(@NotNull E event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            eventPublisher.publishEvent(event);
        } else {
            handleEvictEvent(event);
        }
    }

    public abstract void handleEvictEvent(E event);

}
