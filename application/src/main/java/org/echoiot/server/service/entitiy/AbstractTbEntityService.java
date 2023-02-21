package org.echoiot.server.service.entitiy;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.alarm.AlarmInfo;
import org.echoiot.server.common.data.alarm.AlarmQuery;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.AlarmId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.TimePageLink;
import org.echoiot.server.dao.alarm.AlarmService;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.edge.EdgeService;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.service.executors.DbCallbackExecutorService;
import org.echoiot.server.service.sync.vc.EntitiesVersionControlService;
import org.echoiot.server.service.telemetry.AlarmSubscriptionService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractTbEntityService {

    @Value("${server.log_controller_error_stack_trace}")
    @Getter
    private boolean logControllerErrorStackTrace;

    @Resource
    protected DbCallbackExecutorService dbExecutor;
    @Autowired(required = false)
    protected TbNotificationEntityService notificationEntityService;
    @Autowired(required = false)
    protected EdgeService edgeService;
    @Resource
    protected AlarmService alarmService;
    @Resource
    protected AlarmSubscriptionService alarmSubscriptionService;
    @Resource
    protected CustomerService customerService;
    @Resource
    protected TbClusterService tbClusterService;
    @Autowired(required = false)
    private EntitiesVersionControlService vcService;

    @NotNull
    protected ListenableFuture<Void> removeAlarmsByEntityId(TenantId tenantId, EntityId entityId) {
        ListenableFuture<PageData<AlarmInfo>> alarmsFuture =
                alarmService.findAlarms(tenantId, new AlarmQuery(entityId, new TimePageLink(Integer.MAX_VALUE), null, null, false));

        @NotNull ListenableFuture<List<AlarmId>> alarmIdsFuture = Futures.transform(alarmsFuture, page ->
                page.getData().stream().map(AlarmInfo::getId).collect(Collectors.toList()), dbExecutor);

        return Futures.transform(alarmIdsFuture, ids -> {
            ids.stream().map(alarmId -> alarmService.deleteAlarm(tenantId, alarmId)).collect(Collectors.toList());
            return null;
        }, dbExecutor);
    }

    protected <T> T checkNotNull(@NotNull T reference) throws EchoiotException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    @NotNull
    protected <T> T checkNotNull(@NotNull T reference, String notFoundMessage) throws EchoiotException {
        if (reference == null) {
            throw new EchoiotException(notFoundMessage, EchoiotErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }

    protected <T> T checkNotNull(@NotNull Optional<T> reference) throws EchoiotException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    @NotNull
    protected <T> T checkNotNull(@NotNull Optional<T> reference, String notFoundMessage) throws EchoiotException {
        if (reference.isPresent()) {
            return reference.get();
        } else {
            throw new EchoiotException(notFoundMessage, EchoiotErrorCode.ITEM_NOT_FOUND);
        }
    }

    protected <I extends EntityId> I emptyId(@NotNull EntityType entityType) {
        return (I) EntityIdFactory.getByTypeAndUuid(entityType, ModelConstants.NULL_UUID);
    }

    protected ListenableFuture<UUID> autoCommit(User user, EntityId entityId) throws Exception {
        if (vcService != null) {
            return vcService.autoCommit(user, entityId);
        } else {
            // We do not support auto-commit for rule engine
            return Futures.immediateFailedFuture(new RuntimeException("Operation not supported!"));
        }
    }

    protected ListenableFuture<UUID> autoCommit(User user, EntityType entityType, List<UUID> entityIds) throws Exception {
        if (vcService != null) {
            return vcService.autoCommit(user, entityType, entityIds);
        } else {
            // We do not support auto-commit for rule engine
            return Futures.immediateFailedFuture(new RuntimeException("Operation not supported!"));
        }
    }
}
