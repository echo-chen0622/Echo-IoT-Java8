package org.echoiot.server.controller;

import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.flow.TbRuleChainInputNode;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.EntitySubtype;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeInfo;
import org.echoiot.server.common.data.edge.EdgeSearchQuery;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.echoiot.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.echoiot.server.common.msg.edge.FromEdgeSyncResponse;
import org.echoiot.server.common.msg.edge.ToEdgeSyncRequest;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.exception.IncorrectParameterException;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.edge.EdgeBulkImportService;
import org.echoiot.server.service.entitiy.edge.TbEdgeService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.PerResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.echoiot.server.controller.ControllerConstants.*;

@RestController
@TbCoreComponent
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class EdgeController extends BaseController {
    @NotNull
    private final EdgeBulkImportService edgeBulkImportService;
    @NotNull
    private final TbEdgeService tbEdgeService;

    public static final String EDGE_ID = "edgeId";
    public static final String EDGE_SECURITY_CHECK = "If the user has the authority of 'Tenant Administrator', the server checks that the edge is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the edge is assigned to the same customer.";

    @ApiOperation(value = "Is edges support enabled (isEdgesSupportEnabled)",
            notes = "Returns 'true' if edges support enabled on server, 'false' - otherwise.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edges/enabled", method = RequestMethod.GET)
    @ResponseBody
    public boolean isEdgesSupportEnabled() {
        return edgesEnabled;
    }

    @ApiOperation(value = "Get Edge (getEdgeById)",
            notes = "Get the Edge object based on the provided Edge Id. " + EDGE_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}", method = RequestMethod.GET)
    @ResponseBody
    public Edge getEdgeById(@NotNull @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                            @PathVariable(EDGE_ID) String strEdgeId) throws EchoiotException {
        checkParameter(EDGE_ID, strEdgeId);
        try {
            @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            return checkEdgeId(edgeId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Edge Info (getEdgeInfoById)",
            notes = "Get the Edge Info object based on the provided Edge Id. " + EDGE_SECURITY_CHECK + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/info/{edgeId}", method = RequestMethod.GET)
    @ResponseBody
    public EdgeInfo getEdgeInfoById(@NotNull @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                    @PathVariable(EDGE_ID) String strEdgeId) throws EchoiotException {
        checkParameter(EDGE_ID, strEdgeId);
        try {
            @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            return checkEdgeInfoId(edgeId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Edge (saveEdge)",
            notes = "Create or update the Edge. When creating edge, platform generates Edge Id as " + UUID_WIKI_LINK +
                    "The newly created edge id will be present in the response. " +
                    "Specify existing Edge id to update the edge. " +
                    "Referencing non-existing Edge Id will cause 'Not Found' error." +
                    "\n\nEdge name is unique in the scope of tenant. Use unique identifiers like MAC or IMEI for the edge names and non-unique 'label' field for user-friendly visualization purposes." +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Edge entity. " +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge", method = RequestMethod.POST)
    @ResponseBody
    public Edge saveEdge(@NotNull @ApiParam(value = "A JSON value representing the edge.", required = true)
                         @RequestBody Edge edge) throws Exception {
        TenantId tenantId = getTenantId();
        edge.setTenantId(tenantId);
        boolean created = edge.getId() == null;

        @Nullable RuleChain edgeTemplateRootRuleChain = null;
        if (created) {
            edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(tenantId);
            if (edgeTemplateRootRuleChain == null) {
                throw new DataValidationException("Root edge rule chain is not available!");
            }
        }

        @NotNull Operation operation = created ? Operation.CREATE : Operation.WRITE;

        accessControlService.checkPermission(getCurrentUser(), PerResource.EDGE, operation, edge.getId(), edge);

        return tbEdgeService.save(edge, edgeTemplateRootRuleChain, getCurrentUser());
    }

    @ApiOperation(value = "Delete edge (deleteEdge)",
            notes = "Deletes the edge. Referencing non-existing edge Id will cause an error." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteEdge(@NotNull @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                           @PathVariable(EDGE_ID) String strEdgeId) throws EchoiotException {
        checkParameter(EDGE_ID, strEdgeId);
        @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.DELETE);
        tbEdgeService.delete(edge, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Edges (getEdges)",
            notes = "Returns a page of edges owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edges", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Edge> getEdges(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                   @RequestParam int pageSize,
                                   @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                   @RequestParam int page,
                                   @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
                                   @RequestParam(required = false) String textSearch,
                                   @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                   @RequestParam(required = false) String sortProperty,
                                   @NotNull @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                   @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(edgeService.findEdgesByTenantId(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Assign edge to customer (assignEdgeToCustomer)",
            notes = "Creates assignment of the edge to customer. Customer will be able to query edge afterwards." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/edge/{edgeId}", method = RequestMethod.POST)
    @ResponseBody
    public Edge assignEdgeToCustomer(@NotNull @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
                                     @PathVariable("customerId") String strCustomerId,
                                     @NotNull @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                     @PathVariable(EDGE_ID) String strEdgeId) throws EchoiotException {
        checkParameter("customerId", strCustomerId);
        checkParameter(EDGE_ID, strEdgeId);
        @NotNull CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.ASSIGN_TO_CUSTOMER);
        return tbEdgeService.assignEdgeToCustomer(getTenantId(), edgeId, customer, getCurrentUser());
    }

    @ApiOperation(value = "Unassign edge from customer (unassignEdgeFromCustomer)",
            notes = "Clears assignment of the edge to customer. Customer will not be able to query edge afterwards." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/edge/{edgeId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Edge unassignEdgeFromCustomer(@NotNull @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                         @PathVariable(EDGE_ID) String strEdgeId) throws EchoiotException {
        checkParameter(EDGE_ID, strEdgeId);
        @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.UNASSIGN_FROM_CUSTOMER);
        if (edge.getCustomerId() == null || edge.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            throw new IncorrectParameterException("Edge isn't assigned to any customer!");
        }
        Customer customer = checkCustomerId(edge.getCustomerId(), Operation.READ);

        return tbEdgeService.unassignEdgeFromCustomer(edge, customer, getCurrentUser());
    }

    @ApiOperation(value = "Make edge publicly available (assignEdgeToPublicCustomer)",
            notes = "Edge will be available for non-authorized (not logged-in) users. " +
                    "This is useful to create dashboards that you plan to share/embed on a publicly available website. " +
                    "However, users that are logged-in and belong to different tenant will not be able to access the edge." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/edge/{edgeId}", method = RequestMethod.POST)
    @ResponseBody
    public Edge assignEdgeToPublicCustomer(@NotNull @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                           @PathVariable(EDGE_ID) String strEdgeId) throws EchoiotException {
        checkParameter(EDGE_ID, strEdgeId);
        @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.ASSIGN_TO_CUSTOMER);
        return tbEdgeService.assignEdgeToPublicCustomer(getTenantId(), edgeId, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Edges (getTenantEdges)",
            notes = "Returns a page of edges owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/edges", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Edge> getTenantEdges(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Nullable @ApiParam(value = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(edgeService.findEdgesByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(edgeService.findEdgesByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Edge Infos (getTenantEdgeInfos)",
            notes = "Returns a page of edges info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + EDGE_INFO_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/edgeInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EdgeInfo> getTenantEdgeInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Nullable @ApiParam(value = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(edgeService.findEdgeInfosByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(edgeService.findEdgeInfosByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Edge (getTenantEdge)",
            notes = "Requested edge must be owned by tenant or customer that the user belongs to. " +
                    "Edge name is an unique property of edge. So it can be used to identify the edge." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/edges", params = {"edgeName"}, method = RequestMethod.GET)
    @ResponseBody
    public Edge getTenantEdge(@ApiParam(value = "Unique name of the edge", required = true)
                              @RequestParam String edgeName) throws EchoiotException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(edgeService.findEdgeByTenantIdAndName(tenantId, edgeName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Set root rule chain for provided edge (setEdgeRootRuleChain)",
            notes = "Change root rule chain of the edge to the new provided rule chain. \n" +
                    "This operation will send a notification to update root rule chain on remote edge service." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/{ruleChainId}/root", method = RequestMethod.POST)
    @ResponseBody
    public Edge setEdgeRootRuleChain(@NotNull @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                     @PathVariable(EDGE_ID) String strEdgeId,
                                     @NotNull @ApiParam(value = RULE_CHAIN_ID_PARAM_DESCRIPTION, required = true)
                                     @PathVariable("ruleChainId") String strRuleChainId) throws Exception {
        checkParameter(EDGE_ID, strEdgeId);
        checkParameter("ruleChainId", strRuleChainId);
        @NotNull RuleChainId ruleChainId = new RuleChainId(toUUID(strRuleChainId));
        checkRuleChain(ruleChainId, Operation.WRITE);
        @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.WRITE);
        accessControlService.checkPermission(getCurrentUser(), PerResource.EDGE, Operation.WRITE, edge.getId(), edge);
        return tbEdgeService.setEdgeRootRuleChain(edge, ruleChainId, getCurrentUser());
    }

    @ApiOperation(value = "Get Customer Edges (getCustomerEdges)",
            notes = "Returns a page of edges objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/edges", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Edge> getCustomerEdges(
            @NotNull @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable("customerId") String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Nullable @ApiParam(value = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        checkParameter("customerId", strCustomerId);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            @NotNull CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            PageData<Edge> result;
            if (type != null && type.trim().length() > 0) {
                result = edgeService.findEdgesByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink);
            } else {
                result = edgeService.findEdgesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            }
            return checkNotNull(result);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Edge Infos (getCustomerEdgeInfos)",
            notes = "Returns a page of edges info objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + EDGE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/edgeInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EdgeInfo> getCustomerEdgeInfos(
            @NotNull @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable("customerId") String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Nullable @ApiParam(value = EDGE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = EDGE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        checkParameter("customerId", strCustomerId);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            @NotNull CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            PageData<EdgeInfo> result;
            if (type != null && type.trim().length() > 0) {
                result = edgeService.findEdgeInfosByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink);
            } else {
                result = edgeService.findEdgeInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            }
            return checkNotNull(result);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Edges By Ids (getEdgesByIds)",
            notes = "Requested edges must be owned by tenant or assigned to customer which user is performing the request." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edges", params = {"edgeIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Edge> getEdgesByIds(
            @NotNull @ApiParam(value = "A list of edges ids, separated by comma ','", required = true)
            @RequestParam("edgeIds") String[] strEdgeIds) throws EchoiotException {
        checkArrayParameter("edgeIds", strEdgeIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            CustomerId customerId = user.getCustomerId();
            @NotNull List<EdgeId> edgeIds = new ArrayList<>();
            for (@NotNull String strEdgeId : strEdgeIds) {
                edgeIds.add(new EdgeId(toUUID(strEdgeId)));
            }
            ListenableFuture<List<Edge>> edgesFuture;
            if (customerId == null || customerId.isNullUid()) {
                edgesFuture = edgeService.findEdgesByTenantIdAndIdsAsync(tenantId, edgeIds);
            } else {
                edgesFuture = edgeService.findEdgesByTenantIdCustomerIdAndIdsAsync(tenantId, customerId, edgeIds);
            }
            List<Edge> edges = edgesFuture.get();
            return checkNotNull(edges);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @NotNull
    @ApiOperation(value = "Find related edges (findByQuery)",
            notes = "Returns all edges that are related to the specific entity. " +
                    "The entity id, relation type, edge types, depth of the search, and other query parameters defined using complex 'EdgeSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edges", method = RequestMethod.POST)
    @ResponseBody
    public List<Edge> findByQuery(@NotNull @RequestBody EdgeSearchQuery query) throws EchoiotException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getEdgeTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            List<Edge> edges = checkNotNull(edgeService.findEdgesByQuery(tenantId, query).get());
            edges = edges.stream().filter(edge -> {
                try {
                    accessControlService.checkPermission(user, PerResource.EDGE, Operation.READ, edge.getId(), edge);
                    return true;
                } catch (EchoiotException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            return edges;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Edge Types (getEdgeTypes)",
            notes = "Returns a set of unique edge types based on edges that are either owned by the tenant or assigned to the customer which user is performing the request."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getEdgeTypes() throws EchoiotException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> edgeTypes = edgeService.findEdgeTypesByTenantId(tenantId);
            return checkNotNull(edgeTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @NotNull
    @ApiOperation(value = "Sync edge (syncEdge)",
            notes = "Starts synchronization process between edge and cloud. \n" +
                    "All entities that are assigned to particular edge are going to be send to remote edge service." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/sync/{edgeId}", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> syncEdge(@NotNull @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                         @PathVariable("edgeId") String strEdgeId) throws EchoiotException {
        checkParameter("edgeId", strEdgeId);
        try {
            @NotNull final DeferredResult<ResponseEntity> response = new DeferredResult<>();
            if (isEdgesEnabled()) {
                EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
                edgeId = checkNotNull(edgeId);
                SecurityUser user = getCurrentUser();
                TenantId tenantId = user.getTenantId();
                @NotNull ToEdgeSyncRequest request = new ToEdgeSyncRequest(UUID.randomUUID(), tenantId, edgeId);
                edgeRpcService.processSyncRequest(request, fromEdgeSyncResponse -> reply(response, fromEdgeSyncResponse));
            } else {
                throw new EchoiotException("Edges support disabled", EchoiotErrorCode.GENERAL);
            }
            return response;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void reply(@NotNull DeferredResult<ResponseEntity> response, @NotNull FromEdgeSyncResponse fromEdgeSyncResponse) {
        if (fromEdgeSyncResponse.isSuccess()) {
            response.setResult(new ResponseEntity<>(HttpStatus.OK));
        } else {
            response.setErrorResult(new EchoiotException("Edge is not connected", EchoiotErrorCode.GENERAL));
        }
    }

    @ApiOperation(value = "Find missing rule chains (findMissingToRelatedRuleChains)",
            notes = "Returns list of rule chains ids that are not assigned to particular edge, but these rule chains are present in the already assigned rule chains to edge." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/missingToRelatedRuleChains/{edgeId}", method = RequestMethod.GET)
    @ResponseBody
    public String findMissingToRelatedRuleChains(@NotNull @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
                                                 @PathVariable("edgeId") String strEdgeId) throws EchoiotException {
        try {
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            edgeId = checkNotNull(edgeId);
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            return edgeService.findMissingToRelatedRuleChains(tenantId, edgeId, TbRuleChainInputNode.class.getName());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @NotNull
    @ApiOperation(value = "Import the bulk of edges (processEdgesBulkImport)",
            notes = "There's an ability to import the bulk of edges using the only .csv file." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping("/edge/bulk_import")
    public BulkImportResult<Edge> processEdgesBulkImport(@NotNull @RequestBody BulkImportRequest request) throws Exception {
        SecurityUser user = getCurrentUser();
        RuleChain edgeTemplateRootRuleChain = ruleChainService.getEdgeTemplateRootRuleChain(user.getTenantId());
        if (edgeTemplateRootRuleChain == null) {
            throw new DataValidationException("Root edge rule chain is not available!");
        }

        return edgeBulkImportService.processBulkImport(request, user);
    }
}
