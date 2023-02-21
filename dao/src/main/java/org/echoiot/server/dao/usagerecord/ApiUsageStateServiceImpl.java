package org.echoiot.server.dao.usagerecord;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.id.ApiUsageStateId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.BasicTsKvEntry;
import org.echoiot.server.common.data.kv.LongDataEntry;
import org.echoiot.server.common.data.kv.StringDataEntry;
import org.echoiot.server.common.data.kv.TsKvEntry;
import org.echoiot.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.echoiot.server.dao.entity.AbstractEntityService;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TenantProfileDao;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.echoiot.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class ApiUsageStateServiceImpl extends AbstractEntityService implements ApiUsageStateService {
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private final ApiUsageStateDao apiUsageStateDao;
    private final TenantProfileDao tenantProfileDao;
    private final TenantService tenantService;
    private final TimeseriesService tsService;
    private final DataValidator<ApiUsageState> apiUsageStateValidator;

    public ApiUsageStateServiceImpl(ApiUsageStateDao apiUsageStateDao, TenantProfileDao tenantProfileDao,
                                    TenantService tenantService, @Lazy TimeseriesService tsService,
                                    DataValidator<ApiUsageState> apiUsageStateValidator) {
        this.apiUsageStateDao = apiUsageStateDao;
        this.tenantProfileDao = tenantProfileDao;
        this.tenantService = tenantService;
        this.tsService = tsService;
        this.apiUsageStateValidator = apiUsageStateValidator;
    }

    @Override
    public void deleteApiUsageStateByTenantId(@NotNull TenantId tenantId) {
        log.trace("Executing deleteUsageRecordsByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        apiUsageStateDao.deleteApiUsageStateByTenantId(tenantId);
    }

    @Override
    public void deleteApiUsageStateByEntityId(@NotNull EntityId entityId) {
        log.trace("Executing deleteApiUsageStateByEntityId [{}]", entityId);
        validateId(entityId.getId(), "Invalid entity id");
        apiUsageStateDao.deleteApiUsageStateByEntityId(entityId);
    }

    @NotNull
    @Override
    public ApiUsageState createDefaultApiUsageState(@NotNull TenantId tenantId, EntityId entityId) {
        entityId = Objects.requireNonNullElse(entityId, tenantId);
        log.trace("Executing createDefaultUsageRecord [{}]", entityId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        @NotNull ApiUsageState apiUsageState = new ApiUsageState();
        apiUsageState.setTenantId(tenantId);
        apiUsageState.setEntityId(entityId);
        apiUsageState.setTransportState(ApiUsageStateValue.ENABLED);
        apiUsageState.setReExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setJsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setDbStorageState(ApiUsageStateValue.ENABLED);
        apiUsageState.setSmsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setEmailExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setAlarmExecState(ApiUsageStateValue.ENABLED);
        apiUsageStateValidator.validate(apiUsageState, ApiUsageState::getTenantId);

        ApiUsageState saved = apiUsageStateDao.save(apiUsageState.getTenantId(), apiUsageState);

        @NotNull List<TsKvEntry> apiUsageStates = new ArrayList<>();
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.TRANSPORT.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.DB.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.RE.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.JS.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.EMAIL.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.SMS.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.ALARM.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        tsService.save(tenantId, saved.getId(), apiUsageStates, 0L);

        if (entityId.getEntityType() == EntityType.TENANT && !entityId.equals(TenantId.SYS_TENANT_ID)) {
            tenantId = (TenantId) entityId;
            Tenant tenant = tenantService.findTenantById(tenantId);
            TenantProfile tenantProfile = tenantProfileDao.findById(tenantId, tenant.getTenantProfileId().getId());
            TenantProfileConfiguration configuration = tenantProfile.getProfileData().getConfiguration();

            @NotNull List<TsKvEntry> profileThresholds = new ArrayList<>();
            for (@NotNull ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
                profileThresholds.add(new BasicTsKvEntry(saved.getCreatedTime(), new LongDataEntry(key.getApiLimitKey(), configuration.getProfileThreshold(key))));
            }
            tsService.save(tenantId, saved.getId(), profileThresholds, 0L);
        }

        return saved;
    }

    @Override
    public ApiUsageState update(@NotNull ApiUsageState apiUsageState) {
        log.trace("Executing save [{}]", apiUsageState.getTenantId());
        validateId(apiUsageState.getTenantId(), INCORRECT_TENANT_ID + apiUsageState.getTenantId());
        validateId(apiUsageState.getId(), "Can't save new usage state. Only update is allowed!");
        return apiUsageStateDao.save(apiUsageState.getTenantId(), apiUsageState);
    }

    @Override
    public ApiUsageState findTenantApiUsageState(@NotNull TenantId tenantId) {
        log.trace("Executing findTenantUsageRecord, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return apiUsageStateDao.findTenantApiUsageState(tenantId.getId());
    }

    @Override
    public ApiUsageState findApiUsageStateByEntityId(@NotNull EntityId entityId) {
        validateId(entityId.getId(), "Invalid entity id");
        return apiUsageStateDao.findApiUsageStateByEntityId(entityId);
    }

    @Override
    public ApiUsageState findApiUsageStateById(@NotNull TenantId tenantId, @NotNull ApiUsageStateId id) {
        log.trace("Executing findApiUsageStateById, tenantId [{}], apiUsageStateId [{}]", tenantId, id);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(id, "Incorrect apiUsageStateId " + id);
        return apiUsageStateDao.findById(tenantId, id.getId());
    }

}
