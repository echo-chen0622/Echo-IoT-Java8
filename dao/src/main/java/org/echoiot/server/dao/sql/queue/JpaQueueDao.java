package org.echoiot.server.dao.sql.queue;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.model.sql.QueueEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.queue.QueueDao;
import org.echoiot.server.dao.sql.JpaAbstractDao;
import org.echoiot.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaQueueDao extends JpaAbstractDao<QueueEntity, Queue> implements QueueDao {

    @Resource
    private QueueRepository queueRepository;

    @NotNull
    @Override
    protected Class<QueueEntity> getEntityClass() {
        return QueueEntity.class;
    }

    @Override
    protected JpaRepository<QueueEntity, UUID> getRepository() {
        return queueRepository;
    }

    @Override
    public Queue findQueueByTenantIdAndTopic(@NotNull TenantId tenantId, String topic) {
        return DaoUtil.getData(queueRepository.findByTenantIdAndTopic(tenantId.getId(), topic));
    }

    @Override
    public Queue findQueueByTenantIdAndName(@NotNull TenantId tenantId, String name) {
        return DaoUtil.getData(queueRepository.findByTenantIdAndName(tenantId.getId(), name));
    }

    @Override
    public List<Queue> findAllByTenantId(@NotNull TenantId tenantId) {
        List<QueueEntity> entities = queueRepository.findByTenantId(tenantId.getId());
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<Queue> findAllMainQueues() {
        @NotNull List<QueueEntity> entities = Lists.newArrayList(queueRepository.findAllByName(DataConstants.MAIN_QUEUE_NAME));
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<Queue> findAllQueues() {
        @NotNull List<QueueEntity> entities = Lists.newArrayList(queueRepository.findAll());
        return DaoUtil.convertDataList(entities);
    }

    @NotNull
    @Override
    public PageData<Queue> findQueuesByTenantId(@NotNull TenantId tenantId, @NotNull PageLink pageLink) {
        return DaoUtil.toPageData(queueRepository
                .findByTenantId(tenantId.getId(), Objects.toString(pageLink.getTextSearch(), ""), DaoUtil.toPageable(pageLink)));
    }
}
