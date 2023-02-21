package org.echoiot.server.dao.tenant;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.cache.TbTransactionalCache;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.TenantInfo;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.TenantProfileId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.asset.AssetProfileService;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.device.DeviceProfileService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.entity.AbstractCachedEntityService;
import org.echoiot.server.dao.ota.OtaPackageService;
import org.echoiot.server.dao.queue.QueueService;
import org.echoiot.server.dao.resource.ResourceService;
import org.echoiot.server.dao.rpc.RpcService;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.PaginatedRemover;
import org.echoiot.server.dao.service.Validator;
import org.echoiot.server.dao.settings.AdminSettingsService;
import org.echoiot.server.dao.usagerecord.ApiUsageStateService;
import org.echoiot.server.dao.user.UserService;
import org.echoiot.server.dao.widget.WidgetsBundleService;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.Resource;
import java.util.List;

import static org.echoiot.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class TenantServiceImpl extends AbstractCachedEntityService<TenantId, Tenant, TenantEvictEvent> implements TenantService {

    private static final String DEFAULT_TENANT_REGION = "Global";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Resource
    private TenantDao tenantDao;

    @Resource
    private TenantProfileService tenantProfileService;

    @Resource
    @Lazy
    private UserService userService;

    @Resource
    private CustomerService customerService;

    @Resource
    private AssetService assetService;

    @Resource
    private AssetProfileService assetProfileService;

    @Resource
    private DeviceService deviceService;

    @Resource
    private DeviceProfileService deviceProfileService;

    @Lazy
    @Resource
    private ApiUsageStateService apiUsageStateService;

    @Resource
    private WidgetsBundleService widgetsBundleService;

    @Resource
    private DashboardService dashboardService;

    @Resource
    private RuleChainService ruleChainService;

    @Resource
    private ResourceService resourceService;

    @Resource
    @Lazy
    private OtaPackageService otaPackageService;

    @Resource
    private RpcService rpcService;

    @Resource
    private DataValidator<Tenant> tenantValidator;

    @Lazy
    @Resource
    private QueueService queueService;

    @Resource
    private AdminSettingsService adminSettingsService;

    @Resource
    protected TbTransactionalCache<TenantId, Boolean> existsTenantCache;

    @TransactionalEventListener(classes = TenantEvictEvent.class)
    @Override
    public void handleEvictEvent(@NotNull TenantEvictEvent event) {
        TenantId tenantId = event.getTenantId();
        cache.evict(tenantId);
        if (event.isInvalidateExists()) {
            existsTenantCache.evict(tenantId);
        }
    }

    @Override
    public Tenant findTenantById(@NotNull TenantId tenantId) {
        log.trace("Executing findTenantById [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);

        return cache.getAndPutInTransaction(tenantId, () -> tenantDao.findById(tenantId, tenantId.getId()), true);
    }

    @Override
    public TenantInfo findTenantInfoById(@NotNull TenantId tenantId) {
        log.trace("Executing findTenantInfoById [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return tenantDao.findTenantInfoById(tenantId, tenantId.getId());
    }

    @Override
    public ListenableFuture<Tenant> findTenantByIdAsync(TenantId callerId, @NotNull TenantId tenantId) {
        log.trace("Executing findTenantByIdAsync [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return tenantDao.findByIdAsync(callerId, tenantId.getId());
    }

    @NotNull
    @Override
    @Transactional
    public Tenant saveTenant(@NotNull Tenant tenant) {
        log.trace("Executing saveTenant [{}]", tenant);
        tenant.setRegion(DEFAULT_TENANT_REGION);
        if (tenant.getTenantProfileId() == null) {
            TenantProfile tenantProfile = this.tenantProfileService.findOrCreateDefaultTenantProfile(TenantId.SYS_TENANT_ID);
            tenant.setTenantProfileId(tenantProfile.getId());
        }
        tenantValidator.validate(tenant, Tenant::getId);
        boolean create = tenant.getId() == null;
        Tenant savedTenant = tenantDao.save(tenant.getId(), tenant);
        publishEvictEvent(new TenantEvictEvent(savedTenant.getId(), create));
        if (tenant.getId() == null) {
            deviceProfileService.createDefaultDeviceProfile(savedTenant.getId());
            assetProfileService.createDefaultAssetProfile(savedTenant.getId());
            apiUsageStateService.createDefaultApiUsageState(savedTenant.getId(), null);
        }
        return savedTenant;
    }

    /**
     * We intentionally leave this method without "Transactional" annotation due to complexity of the method.
     * Ideally we should delete related entites without "paginatedRemover" logic. But in such a case we can't clear cache and send events.
     * We will create separate task to make "deleteTenant" transactional.
     */
    @Override
    public void deleteTenant(@NotNull TenantId tenantId) {
        log.trace("Executing deleteTenant [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        entityViewService.deleteEntityViewsByTenantId(tenantId);
        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);
        assetService.deleteAssetsByTenantId(tenantId);
        assetProfileService.deleteAssetProfilesByTenantId(tenantId);
        deviceService.deleteDevicesByTenantId(tenantId);
        deviceProfileService.deleteDeviceProfilesByTenantId(tenantId);
        dashboardService.deleteDashboardsByTenantId(tenantId);
        customerService.deleteCustomersByTenantId(tenantId);
        edgeService.deleteEdgesByTenantId(tenantId);
        userService.deleteTenantAdmins(tenantId);
        ruleChainService.deleteRuleChainsByTenantId(tenantId);
        apiUsageStateService.deleteApiUsageStateByTenantId(tenantId);
        resourceService.deleteResourcesByTenantId(tenantId);
        otaPackageService.deleteOtaPackagesByTenantId(tenantId);
        rpcService.deleteAllRpcByTenantId(tenantId);
        queueService.deleteQueuesByTenantId(tenantId);
        adminSettingsService.deleteAdminSettingsByTenantId(tenantId);
        tenantDao.removeById(tenantId, tenantId.getId());
        publishEvictEvent(new TenantEvictEvent(tenantId, true));
        deleteEntityRelations(tenantId, tenantId);
    }

    @Override
    public PageData<Tenant> findTenants(PageLink pageLink) {
        log.trace("Executing findTenants pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantDao.findTenants(TenantId.SYS_TENANT_ID, pageLink);
    }

    @Override
    public PageData<TenantInfo> findTenantInfos(PageLink pageLink) {
        log.trace("Executing findTenantInfos pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantDao.findTenantInfos(TenantId.SYS_TENANT_ID, pageLink);
    }

    @Override
    public List<TenantId> findTenantIdsByTenantProfileId(TenantProfileId tenantProfileId) {
        log.trace("Executing findTenantsByTenantProfileId [{}]", tenantProfileId);
        return tenantDao.findTenantIdsByTenantProfileId(tenantProfileId);
    }

    @Override
    public void deleteTenants() {
        log.trace("Executing deleteTenants");
        tenantsRemover.removeEntities(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID);
    }

    @Override
    public PageData<TenantId> findTenantsIds(PageLink pageLink) {
        log.trace("Executing findTenantsIds");
        Validator.validatePageLink(pageLink);
        return tenantDao.findTenantsIds(pageLink);
    }

    @Override
    public boolean tenantExists(@NotNull TenantId tenantId) {
        return existsTenantCache.getAndPutInTransaction(tenantId, () -> tenantDao.existsById(tenantId, tenantId.getId()), false);
    }

    private final PaginatedRemover<TenantId, Tenant> tenantsRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<Tenant> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return tenantDao.findTenants(tenantId, pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, @NotNull Tenant entity) {
            deleteTenant(TenantId.fromUUID(entity.getUuidId()));
        }
    };
}
