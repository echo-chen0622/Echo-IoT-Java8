package org.echoiot.server.dao.sql.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.alarm.*;
import org.echoiot.server.common.data.id.AlarmId;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.query.AlarmData;
import org.echoiot.server.common.data.query.AlarmDataQuery;
import org.echoiot.server.dao.DaoUtil;
import org.echoiot.server.dao.alarm.AlarmDao;
import org.echoiot.server.dao.model.sql.AlarmEntity;
import org.echoiot.server.dao.model.sql.EntityAlarmEntity;
import org.echoiot.server.dao.sql.JpaAbstractDao;
import org.echoiot.server.dao.sql.query.AlarmQueryRepository;
import org.echoiot.server.dao.util.SqlDao;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by Valerii Sosliuk on 5/19/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaAlarmDao extends JpaAbstractDao<AlarmEntity, Alarm> implements AlarmDao {

    @Resource
    private AlarmRepository alarmRepository;

    @Resource
    private AlarmQueryRepository alarmQueryRepository;

    @Resource
    private EntityAlarmRepository entityAlarmRepository;

    @Override
    protected Class<AlarmEntity> getEntityClass() {
        return AlarmEntity.class;
    }

    @Override
    protected JpaRepository<AlarmEntity, UUID> getRepository() {
        return alarmRepository;
    }

    @Nullable
    @Override
    public Alarm findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        List<AlarmEntity> latest = alarmRepository.findLatestByOriginatorAndType(
                originator.getId(),
                type,
                PageRequest.of(0, 1));
        return latest.isEmpty() ? null : DaoUtil.getData(latest.get(0));
    }

    @Override
    public ListenableFuture<Alarm> findLatestByOriginatorAndTypeAsync(TenantId tenantId, EntityId originator, String type) {
        return service.submit(() -> findLatestByOriginatorAndType(tenantId, originator, type));
    }

    @Override
    public Alarm findAlarmById(TenantId tenantId, UUID key) {
        return findById(tenantId, key);
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, UUID key) {
        return findByIdAsync(tenantId, key);
    }

    @Override
    public PageData<AlarmInfo> findAlarms(TenantId tenantId, AlarmQuery query) {
        log.trace("Try to find alarms by entity [{}], status [{}] and pageLink [{}]", query.getAffectedEntityId(), query.getStatus(), query.getPageLink());
        EntityId affectedEntity = query.getAffectedEntityId();
        @Nullable Set<AlarmStatus> statusSet = null;
        if (query.getSearchStatus() != null) {
            statusSet = query.getSearchStatus().getStatuses();
        } else if (query.getStatus() != null) {
            statusSet = Collections.singleton(query.getStatus());
        }
        if (affectedEntity != null) {
            return DaoUtil.toPageData(
                    alarmRepository.findAlarms(
                            tenantId.getId(),
                            affectedEntity.getId(),
                            affectedEntity.getEntityType().name(),
                            query.getPageLink().getStartTime(),
                            query.getPageLink().getEndTime(),
                            statusSet,
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            DaoUtil.toPageable(query.getPageLink())
                    )
            );
        } else {
            return DaoUtil.toPageData(
                    alarmRepository.findAllAlarms(
                            tenantId.getId(),
                            query.getPageLink().getStartTime(),
                            query.getPageLink().getEndTime(),
                            statusSet,
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            DaoUtil.toPageable(query.getPageLink())
                    )
            );
        }
    }

    @Override
    public PageData<AlarmInfo> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query) {
        log.trace("Try to find customer alarms by status [{}] and pageLink [{}]", query.getStatus(), query.getPageLink());
        @Nullable Set<AlarmStatus> statusSet = null;
        if (query.getSearchStatus() != null) {
            statusSet = query.getSearchStatus().getStatuses();
        } else if (query.getStatus() != null) {
            statusSet = Collections.singleton(query.getStatus());
        }
        return DaoUtil.toPageData(
                alarmRepository.findCustomerAlarms(
                        tenantId.getId(),
                        customerId.getId(),
                        query.getPageLink().getStartTime(),
                        query.getPageLink().getEndTime(),
                        statusSet,
                        Objects.toString(query.getPageLink().getTextSearch(), ""),
                        DaoUtil.toPageable(query.getPageLink())
                )
        );
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        return alarmQueryRepository.findAlarmDataByQueryForEntities(tenantId, query, orderedEntityIds);
    }

    @Override
    public Set<AlarmSeverity> findAlarmSeverities(TenantId tenantId, EntityId entityId, Set<AlarmStatus> statuses) {
        return alarmRepository.findAlarmSeverities(tenantId.getId(), entityId.getId(), entityId.getEntityType().name(), statuses);
    }

    @Override
    public PageData<AlarmId> findAlarmsIdsByEndTsBeforeAndTenantId(Long time, TenantId tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(alarmRepository.findAlarmsIdsByEndTsBeforeAndTenantId(time, tenantId.getId(), DaoUtil.toPageable(pageLink)))
                .mapData(AlarmId::new);
    }

    @Override
    public void createEntityAlarmRecord(EntityAlarm entityAlarm) {
        log.debug("Saving entity {}", entityAlarm);
        entityAlarmRepository.save(new EntityAlarmEntity(entityAlarm));
    }

    @Override
    public List<EntityAlarm> findEntityAlarmRecords(TenantId tenantId, AlarmId id) {
        log.trace("[{}] Try to find entity alarm records using [{}]", tenantId, id);
        return DaoUtil.convertDataList(entityAlarmRepository.findAllByAlarmId(id.getId()));
    }

    @Override
    public void deleteEntityAlarmRecords(TenantId tenantId, EntityId entityId) {
        log.trace("[{}] Try to delete entity alarm records using [{}]", tenantId, entityId);
        entityAlarmRepository.deleteByEntityId(entityId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ALARM;
    }

}
