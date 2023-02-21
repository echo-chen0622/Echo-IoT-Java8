package org.echoiot.rule.engine.util;

import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.HasTenantId;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.rule.RuleNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TenantIdLoader {

    @Nullable
    public static TenantId findTenantId(@NotNull TbContext ctx, @NotNull EntityId entityId) {
        UUID id = entityId.getId();
        EntityType entityType = entityId.getEntityType();
        TenantId ctxTenantId = ctx.getTenantId();

        @Nullable HasTenantId tenantEntity;
        switch (entityType) {
            case TENANT:
                return new TenantId(id);
            case CUSTOMER:
                tenantEntity = ctx.getCustomerService().findCustomerById(ctxTenantId, new CustomerId(id));
                break;
            case USER:
                tenantEntity = ctx.getUserService().findUserById(ctxTenantId, new UserId(id));
                break;
            case ASSET:
                tenantEntity = ctx.getAssetService().findAssetById(ctxTenantId, new AssetId(id));
                break;
            case DEVICE:
                tenantEntity = ctx.getDeviceService().findDeviceById(ctxTenantId, new DeviceId(id));
                break;
            case ALARM:
                tenantEntity = ctx.getAlarmService().findAlarmById(ctxTenantId, new AlarmId(id));
                break;
            case RULE_CHAIN:
                tenantEntity = ctx.getRuleChainService().findRuleChainById(ctxTenantId, new RuleChainId(id));
                break;
            case ENTITY_VIEW:
                tenantEntity = ctx.getEntityViewService().findEntityViewById(ctxTenantId, new EntityViewId(id));
                break;
            case DASHBOARD:
                tenantEntity = ctx.getDashboardService().findDashboardById(ctxTenantId, new DashboardId(id));
                break;
            case EDGE:
                tenantEntity = ctx.getEdgeService().findEdgeById(ctxTenantId, new EdgeId(id));
                break;
            case OTA_PACKAGE:
                tenantEntity = ctx.getOtaPackageService().findOtaPackageInfoById(ctxTenantId, new OtaPackageId(id));
                break;
            case ASSET_PROFILE:
                tenantEntity = ctx.getAssetProfileCache().get(ctxTenantId, new AssetProfileId(id));
                break;
            case DEVICE_PROFILE:
                tenantEntity = ctx.getDeviceProfileCache().get(ctxTenantId, new DeviceProfileId(id));
                break;
            case WIDGET_TYPE:
                tenantEntity = ctx.getWidgetTypeService().findWidgetTypeById(ctxTenantId, new WidgetTypeId(id));
                break;
            case WIDGETS_BUNDLE:
                tenantEntity = ctx.getWidgetBundleService().findWidgetsBundleById(ctxTenantId, new WidgetsBundleId(id));
                break;
            case RPC:
                tenantEntity = ctx.getRpcService().findRpcById(ctxTenantId, new RpcId(id));
                break;
            case QUEUE:
                tenantEntity = ctx.getQueueService().findQueueById(ctxTenantId, new QueueId(id));
                break;
            case API_USAGE_STATE:
                tenantEntity = ctx.getRuleEngineApiUsageStateService().findApiUsageStateById(ctxTenantId, new ApiUsageStateId(id));
                break;
            case TB_RESOURCE:
                tenantEntity = ctx.getResourceService().findResourceInfoById(ctxTenantId, new TbResourceId(id));
                break;
            case RULE_NODE:
                RuleNode ruleNode = ctx.getRuleChainService().findRuleNodeById(ctxTenantId, new RuleNodeId(id));
                if (ruleNode != null) {
                    tenantEntity = ctx.getRuleChainService().findRuleChainById(ctxTenantId, ruleNode.getRuleChainId());
                } else {
                    tenantEntity = null;
                }
                break;
            case TENANT_PROFILE:
                if (ctx.getTenantProfile().getId().equals(entityId)) {
                    return ctxTenantId;
                } else {
                    tenantEntity = null;
                }
                break;
            default:
                throw new RuntimeException("Unexpected entity type: " + entityId.getEntityType());
        }
        return tenantEntity != null ? tenantEntity.getTenantId() : null;
    }

}
