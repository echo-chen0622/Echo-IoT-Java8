package org.echoiot.server.service.entitiy.edge;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.dao.rule.RuleChainService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.edge.EdgeNotificationService;
import org.echoiot.server.service.entitiy.AbstractTbEntityService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbEdgeService extends AbstractTbEntityService implements TbEdgeService {

    @NotNull
    private final EdgeNotificationService edgeNotificationService;
    @NotNull
    private final RuleChainService ruleChainService;

    @NotNull
    @Override
    public Edge save(@NotNull Edge edge, @NotNull RuleChain edgeTemplateRootRuleChain, User user) throws Exception {
        @NotNull ActionType actionType = edge.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = edge.getTenantId();
        try {
            if (actionType == ActionType.ADDED && edge.getRootRuleChainId() == null) {
                edge.setRootRuleChainId(edgeTemplateRootRuleChain.getId());
            }
            Edge savedEdge = checkNotNull(edgeService.saveEdge(edge));
            EdgeId edgeId = savedEdge.getId();

            if (actionType == ActionType.ADDED) {
                ruleChainService.assignRuleChainToEdge(tenantId, edgeTemplateRootRuleChain.getId(), edgeId);
                edgeNotificationService.setEdgeRootRuleChain(tenantId, savedEdge, edgeTemplateRootRuleChain.getId());
                edgeService.assignDefaultRuleChainsToEdge(tenantId, edgeId);
            }

            notificationEntityService.notifyCreateOrUpdateOrDeleteEdge(tenantId, edgeId, savedEdge.getCustomerId(), savedEdge, actionType, user);

            return savedEdge;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE), edge, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(@NotNull Edge edge, User user) {
        EdgeId edgeId = edge.getId();
        TenantId tenantId = edge.getTenantId();
        try {
            edgeService.deleteEdge(tenantId, edgeId);
            notificationEntityService.notifyCreateOrUpdateOrDeleteEdge(tenantId, edgeId, edge.getCustomerId(), edge, ActionType.DELETED, user, edgeId.toString());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE), ActionType.DELETED,
                    user, e, edgeId.toString());
            throw e;
        }
    }

    @Override
    public Edge assignEdgeToCustomer(TenantId tenantId, @NotNull EdgeId edgeId, @NotNull Customer customer, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            Edge savedEdge = checkNotNull(edgeService.assignEdgeToCustomer(tenantId, edgeId, customerId));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, edgeId, customerId, savedEdge,
                    actionType, user, true, edgeId.toString(), customerId.toString(), customer.getName());

            return savedEdge;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE),
                    ActionType.ASSIGNED_TO_CUSTOMER, user, e, edgeId.toString(), customerId.toString());
            throw e;
        }
    }

    @Override
    public Edge unassignEdgeFromCustomer(@NotNull Edge edge, @NotNull Customer customer, User user) throws EchoiotException {
        @NotNull ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = edge.getTenantId();
        EdgeId edgeId = edge.getId();
        CustomerId customerId = customer.getId();
        try {
            Edge savedEdge = checkNotNull(edgeService.unassignEdgeFromCustomer(tenantId, edgeId));
            notificationEntityService.notifyAssignOrUnassignEntityToCustomer(tenantId, edgeId, customerId, savedEdge,
                    actionType, user, true, edgeId.toString(), customerId.toString(), customer.getName());
            return savedEdge;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE),
                    ActionType.UNASSIGNED_FROM_CUSTOMER, user, e, edgeId.toString());
            throw e;
        }
    }

    @Override
    public Edge assignEdgeToPublicCustomer(TenantId tenantId, @NotNull EdgeId edgeId, User user) throws EchoiotException {
        Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
        CustomerId customerId = publicCustomer.getId();
        try {
            Edge savedEdge = checkNotNull(edgeService.assignEdgeToCustomer(tenantId, edgeId, customerId));

            notificationEntityService.notifyCreateOrUpdateOrDeleteEdge(tenantId, edgeId, customerId, savedEdge, ActionType.ASSIGNED_TO_CUSTOMER, user,
                    edgeId.toString(), customerId.toString(), publicCustomer.getName());

            return savedEdge;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE),
                    ActionType.ASSIGNED_TO_CUSTOMER, user, e, edgeId.toString());
            throw e;
        }
    }

    @Override
    public Edge setEdgeRootRuleChain(@NotNull Edge edge, RuleChainId ruleChainId, User user) throws Exception {
        TenantId tenantId = edge.getTenantId();
        EdgeId edgeId = edge.getId();
        try {
            Edge updatedEdge = edgeNotificationService.setEdgeRootRuleChain(tenantId, edge, ruleChainId);
            notificationEntityService.logEntityAction(tenantId, edgeId, edge, null, ActionType.UPDATED, user);
            return updatedEdge;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE),
                    ActionType.UPDATED, user, e, edgeId.toString());
            throw e;
        }
    }
}
