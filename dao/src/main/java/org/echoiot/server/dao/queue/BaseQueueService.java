package org.echoiot.server.dao.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.QueueId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.dao.entity.AbstractEntityService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.PaginatedRemover;
import org.echoiot.server.dao.service.Validator;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaseQueueService extends AbstractEntityService implements QueueService {

    @Resource
    private QueueDao queueDao;

    @Lazy
    @Resource
    private TbTenantProfileCache tenantProfileCache;

    @Resource
    private DataValidator<Queue> queueValidator;

//    @Resource
//    private QueueStatsService queueStatsService;

    @Override
    public Queue saveQueue(@NotNull Queue queue) {
        log.trace("Executing createOrUpdateQueue [{}]", queue);
        queueValidator.validate(queue, Queue::getTenantId);
        return queueDao.save(queue.getTenantId(), queue);
    }

    @Override
    public void deleteQueue(TenantId tenantId, @NotNull QueueId queueId) {
        log.trace("Executing deleteQueue, queueId: [{}]", queueId);
        try {
            queueDao.removeById(tenantId, queueId.getId());
        } catch (Exception t) {
            @Nullable ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_default_queue_device_profile")) {
                throw new DataValidationException("The queue referenced by the device profiles cannot be deleted!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public List<Queue> findQueuesByTenantId(@NotNull TenantId tenantId) {
        log.trace("Executing findQueues, tenantId: [{}]", tenantId);
        return queueDao.findAllByTenantId(getSystemOrIsolatedTenantId(tenantId));
    }

    @Override
    public PageData<Queue> findQueuesByTenantId(@NotNull TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findQueues pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return queueDao.findQueuesByTenantId(getSystemOrIsolatedTenantId(tenantId), pageLink);
    }

    @Override
    public List<Queue> findAllQueues() {
        log.trace("Executing findAllQueues");
        return queueDao.findAllQueues();
    }

    @Override
    public Queue findQueueById(TenantId tenantId, @NotNull QueueId queueId) {
        log.trace("Executing findQueueById, queueId: [{}]", queueId);
        return queueDao.findById(tenantId, queueId.getId());
    }

    @Override
    public Queue findQueueByTenantIdAndName(@NotNull TenantId tenantId, String queueName) {
        log.trace("Executing findQueueByTenantIdAndName, tenantId: [{}] queueName: [{}]", tenantId, queueName);
        return queueDao.findQueueByTenantIdAndName(getSystemOrIsolatedTenantId(tenantId), queueName);
    }

    @Override
    public Queue findQueueByTenantIdAndNameInternal(TenantId tenantId, String queueName) {
        log.trace("Executing findQueueByTenantIdAndNameInternal, tenantId: [{}] queueName: [{}]", tenantId, queueName);
        return queueDao.findQueueByTenantIdAndName(tenantId, queueName);
    }

    @Override
    public void deleteQueuesByTenantId(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for delete queues request.");
        tenantQueuesRemover.removeEntities(tenantId, tenantId);
    }

    private final PaginatedRemover<TenantId, Queue> tenantQueuesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<Queue> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return queueDao.findQueuesByTenantId(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, @NotNull Queue entity) {
                    deleteQueue(tenantId, entity.getId());
                }
            };

    @Nullable
    private TenantId getSystemOrIsolatedTenantId(@NotNull TenantId tenantId) {
        if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
            TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
            if (tenantProfile.isIsolatedTbRuleEngine()) {
                return tenantId;
            }
        }

        return TenantId.SYS_TENANT_ID;
    }
}
