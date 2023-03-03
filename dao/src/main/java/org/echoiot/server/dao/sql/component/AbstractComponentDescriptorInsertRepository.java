package org.echoiot.server.dao.sql.component;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.model.sql.ComponentDescriptorEntity;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

@Slf4j
public abstract class AbstractComponentDescriptorInsertRepository implements ComponentDescriptorInsertRepository {

    @PersistenceContext
    protected EntityManager entityManager;

    @Resource
    protected PlatformTransactionManager transactionManager;

    /**
     * 保存组件方法。
     * 此方法用了手动事务，整体实现未走 mybatis。感觉应该是框架太老导致的。可以重构，换成 mybatis 框架
     *
     * @param entity
     * @param insertOrUpdateOnPrimaryKeyConflict
     * @param insertOrUpdateOnUniqueKeyConflict
     */
    @Nullable
    protected ComponentDescriptorEntity saveAndGet(ComponentDescriptorEntity entity, String insertOrUpdateOnPrimaryKeyConflict, String insertOrUpdateOnUniqueKeyConflict) {
        @Nullable ComponentDescriptorEntity componentDescriptorEntity = null;
        //这里是用了一个手动事务策略，我认为，这里可以改造成自动事务。
        TransactionStatus insertTransaction = getTransactionStatus(TransactionDefinition.PROPAGATION_REQUIRED);
        try {
            //尝试用主键插入
            componentDescriptorEntity = processSaveOrUpdate(entity, insertOrUpdateOnPrimaryKeyConflict);
            transactionManager.commit(insertTransaction);
        } catch (Throwable throwable) {
            transactionManager.rollback(insertTransaction);
            if (throwable.getCause() instanceof ConstraintViolationException) {
                log.trace("插入请求导致违反定义的完整性约束 {} 的组件描述符，其 ID 为 {}、名称 {} 和实体类型 {}", throwable.getMessage(), entity.getUuid(), entity.getName(), entity.getType());
                // 重启事务，尝试用唯一键插入
                TransactionStatus transaction = getTransactionStatus(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                try {
                    componentDescriptorEntity = processSaveOrUpdate(entity, insertOrUpdateOnUniqueKeyConflict);
                    transactionManager.commit(transaction);
                } catch (Throwable th) {
                    log.trace("无法执行 ID 为 {}、名称 {} 和实体类型 {} 的组件描述符的执行语句", entity.getUuid(), entity.getName(), entity.getType());
                    transactionManager.rollback(transaction);
                }
            } else {
                log.trace("无法执行 id 为 {}、名称 {} 和实体类型 {} 的组件描述符的插入语句", entity.getUuid(), entity.getName(), entity.getType());
            }
        }
        return componentDescriptorEntity;
    }

    @Modifying
    protected abstract ComponentDescriptorEntity doProcessSaveOrUpdate(ComponentDescriptorEntity entity, String query);

    protected Query getQuery(ComponentDescriptorEntity entity, String query) {
        return entityManager.createNativeQuery(query, ComponentDescriptorEntity.class)
                            .setParameter("id", entity.getUuid())
                            .setParameter("created_time", entity.getCreatedTime())
                            .setParameter("clazz", entity.getClazz())
                            .setParameter("configuration_descriptor", entity.getConfigurationDescriptor().toString())
                            .setParameter("name", entity.getName())
                            .setParameter("scope", entity.getScope().name())
                            .setParameter("search_text", entity.getSearchText())
                            .setParameter("type", entity.getType().name());
    }

    private ComponentDescriptorEntity processSaveOrUpdate(ComponentDescriptorEntity entity, String query) {
        return doProcessSaveOrUpdate(entity, query);
    }

    private TransactionStatus getTransactionStatus(int propagationRequired) {
        DefaultTransactionDefinition insertDefinition = new DefaultTransactionDefinition();
        insertDefinition.setPropagationBehavior(propagationRequired);
        return transactionManager.getTransaction(insertDefinition);
    }
}
