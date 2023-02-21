package org.echoiot.rule.engine.action;

import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;

@RuleNode(
        type = ComponentType.ACTION,
        name = "unassign from customer",
        configClazz = TbUnassignFromCustomerNodeConfiguration.class,
        nodeDescription = "Unassign Message Originator Entity from Customer",
        nodeDetails = "Finds target Entity Customer by Customer name pattern and then unassign Originator Entity from this customer.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeUnAssignToCustomerConfig",
        icon = "remove_circle"
)
public class TbUnassignFromCustomerNode extends TbAbstractCustomerActionNode<TbUnassignFromCustomerNodeConfiguration> {

    @Override
    protected boolean createCustomerIfNotExists() {
        return false;
    }

    @Override
    protected TbUnassignFromCustomerNodeConfiguration loadCustomerNodeActionConfig(@NotNull TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbUnassignFromCustomerNodeConfiguration.class);
    }

    @Override
    protected void doProcessCustomerAction(@NotNull TbContext ctx, @NotNull TbMsg msg, CustomerId customerId) {
        EntityType originatorType = msg.getOriginator().getEntityType();
        switch (originatorType) {
            case DEVICE:
                processUnnasignDevice(ctx, msg);
                break;
            case ASSET:
                processUnnasignAsset(ctx, msg);
                break;
            case ENTITY_VIEW:
                processUnassignEntityView(ctx, msg);
                break;
            case EDGE:
                processUnassignEdge(ctx, msg);
                break;
            case DASHBOARD:
                processUnnasignDashboard(ctx, msg, customerId);
                break;
            default:
                ctx.tellFailure(msg, new RuntimeException("Unsupported originator type '" + originatorType +
                        "'! Only 'DEVICE', 'ASSET',  'ENTITY_VIEW' or 'DASHBOARD' types are allowed."));
                break;
        }
    }

    private void processUnnasignAsset(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        ctx.getAssetService().unassignAssetFromCustomer(ctx.getTenantId(), new AssetId(msg.getOriginator().getId()));
    }

    private void processUnnasignDevice(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        ctx.getDeviceService().unassignDeviceFromCustomer(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId()));
    }

    private void processUnnasignDashboard(@NotNull TbContext ctx, @NotNull TbMsg msg, CustomerId customerId) {
        ctx.getDashboardService().unassignDashboardFromCustomer(ctx.getTenantId(), new DashboardId(msg.getOriginator().getId()), customerId);
    }

    private void processUnassignEntityView(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        ctx.getEntityViewService().unassignEntityViewFromCustomer(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId()));
    }

    private void processUnassignEdge(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        ctx.getEdgeService().unassignEdgeFromCustomer(ctx.getTenantId(), new EdgeId(msg.getOriginator().getId()));
    }
}
