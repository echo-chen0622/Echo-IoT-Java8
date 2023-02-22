package org.echoiot.server.controller;

import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.EntitySubtype;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.common.data.EntityViewInfo;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.entityview.EntityViewSearchQuery;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.EntityViewId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.page.TimePageLink;
import org.echoiot.server.dao.exception.IncorrectParameterException;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.entityview.TbEntityViewService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.PerResource;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Victor Basanets on 8/28/2017.
 */
@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class EntityViewController extends BaseController {

    public final TbEntityViewService tbEntityViewService;

    public static final String ENTITY_VIEW_ID = "entityViewId";

    @ApiOperation(value = "Get entity view (getEntityViewById)",
            notes = "Fetch the EntityView object based on the provided entity view id. "
                    + ControllerConstants.ENTITY_VIEW_DESCRIPTION + ControllerConstants.MODEL_DESCRIPTION + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityView/{entityViewId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityView getEntityViewById(
            @ApiParam(value = ControllerConstants.ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws EchoiotException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        try {
            return checkEntityViewId(new EntityViewId(toUUID(strEntityViewId)), Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Entity View info (getEntityViewInfoById)",
            notes = "Fetch the Entity View info object based on the provided Entity View Id. "
                    + ControllerConstants.ENTITY_VIEW_INFO_DESCRIPTION + ControllerConstants.MODEL_DESCRIPTION + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityView/info/{entityViewId}", method = RequestMethod.GET)
    @ResponseBody
    public EntityViewInfo getEntityViewInfoById(
            @ApiParam(value = ControllerConstants.ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws EchoiotException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        try {
            EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
            return checkEntityViewInfoId(entityViewId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Save or update entity view (saveEntityView)",
            notes = ControllerConstants.ENTITY_VIEW_DESCRIPTION + ControllerConstants.MODEL_DESCRIPTION +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Entity View entity." +
                    ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityView", method = RequestMethod.POST)
    @ResponseBody
    public EntityView saveEntityView(
            @ApiParam(value = "A JSON object representing the entity view.")
            @RequestBody EntityView entityView) throws Exception {
        entityView.setTenantId(getCurrentUser().getTenantId());
        @Nullable EntityView existingEntityView = null;
        if (entityView.getId() == null) {
            accessControlService
                    .checkPermission(getCurrentUser(), PerResource.ENTITY_VIEW, Operation.CREATE, null, entityView);
        } else {
            existingEntityView = checkEntityViewId(entityView.getId(), Operation.WRITE);
        }
        return tbEntityViewService.save(entityView, existingEntityView, getCurrentUser());
    }

    @ApiOperation(value = "Delete entity view (deleteEntityView)",
            notes = "Delete the EntityView object based on the provided entity view id. "
                    + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/entityView/{entityViewId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteEntityView(
            @ApiParam(value = ControllerConstants.ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws EchoiotException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
        EntityView entityView = checkEntityViewId(entityViewId, Operation.DELETE);
        tbEntityViewService.delete(entityView, getCurrentUser());
    }

    @ApiOperation(value = "Get Entity View by name (getTenantEntityView)",
            notes = "Fetch the Entity View object based on the tenant id and entity view name. " + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/entityViews", params = {"entityViewName"}, method = RequestMethod.GET)
    @ResponseBody
    public EntityView getTenantEntityView(
            @ApiParam(value = "Entity View name")
            @RequestParam String entityViewName) throws EchoiotException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(entityViewService.findEntityViewByTenantIdAndName(tenantId, entityViewName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Assign Entity View to customer (assignEntityViewToCustomer)",
            notes = "Creates assignment of the Entity View to customer. Customer will be able to query Entity View afterwards." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/entityView/{entityViewId}", method = RequestMethod.POST)
    @ResponseBody
    public EntityView assignEntityViewToCustomer(
            @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(ControllerConstants.CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = ControllerConstants.ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws EchoiotException {
        checkParameter(ControllerConstants.CUSTOMER_ID, strCustomerId);
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);

        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);

        EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
        checkEntityViewId(entityViewId, Operation.ASSIGN_TO_CUSTOMER);

        return tbEntityViewService.assignEntityViewToCustomer(getTenantId(), entityViewId, customer, getCurrentUser());
    }

    @ApiOperation(value = "Unassign Entity View from customer (unassignEntityViewFromCustomer)",
            notes = "Clears assignment of the Entity View to customer. Customer will not be able to query Entity View afterwards." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/entityView/{entityViewId}", method = RequestMethod.DELETE)
    @ResponseBody
    public EntityView unassignEntityViewFromCustomer(
            @ApiParam(value = ControllerConstants.ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws EchoiotException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
        EntityView entityView = checkEntityViewId(entityViewId, Operation.UNASSIGN_FROM_CUSTOMER);
        if (entityView.getCustomerId() == null || entityView.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            throw new IncorrectParameterException("Entity View isn't assigned to any customer!");
        }

        Customer customer = checkCustomerId(entityView.getCustomerId(), Operation.READ);

        return tbEntityViewService.unassignEntityViewFromCustomer(getTenantId(), entityViewId, customer, getCurrentUser());
    }

    @ApiOperation(value = "Get Customer Entity Views (getCustomerEntityViews)",
            notes = "Returns a page of Entity View objects assigned to customer. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/entityViews", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityView> getCustomerEntityViews(
            @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ControllerConstants.CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Nullable @ApiParam(value = ControllerConstants.ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @ApiParam(value = ControllerConstants.ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.ENTITY_VIEW_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        checkParameter(ControllerConstants.CUSTOMER_ID, strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(entityViewService.findEntityViewsByTenantIdAndCustomerIdAndType(tenantId, customerId, pageLink, type));
            } else {
                return checkNotNull(entityViewService.findEntityViewsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Entity View info (getCustomerEntityViewInfos)",
            notes = "Returns a page of Entity View info objects assigned to customer. " + ControllerConstants.ENTITY_VIEW_DESCRIPTION +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/entityViewInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityViewInfo> getCustomerEntityViewInfos(
            @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ControllerConstants.CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Nullable @ApiParam(value = ControllerConstants.ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @ApiParam(value = ControllerConstants.ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.ENTITY_VIEW_INFO_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Entity Views (getTenantEntityViews)",
            notes = "Returns a page of entity views owned by tenant. " + ControllerConstants.ENTITY_VIEW_DESCRIPTION +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/entityViews", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityView> getTenantEntityViews(
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Nullable @ApiParam(value = ControllerConstants.ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @ApiParam(value = ControllerConstants.ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.ENTITY_VIEW_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);

            if (type != null && type.trim().length() > 0) {
                return checkNotNull(entityViewService.findEntityViewByTenantIdAndType(tenantId, pageLink, type));
            } else {
                return checkNotNull(entityViewService.findEntityViewByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Entity Views (getTenantEntityViews)",
            notes = "Returns a page of entity views info owned by tenant. " + ControllerConstants.ENTITY_VIEW_DESCRIPTION +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/entityViewInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityViewInfo> getTenantEntityViewInfos(
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Nullable @ApiParam(value = ControllerConstants.ENTITY_VIEW_TYPE)
            @RequestParam(required = false) String type,
            @ApiParam(value = ControllerConstants.ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.ENTITY_VIEW_INFO_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(entityViewService.findEntityViewInfosByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(entityViewService.findEntityViewInfosByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find related entity views (findByQuery)",
            notes = "Returns all entity views that are related to the specific entity. " +
                    "The entity id, relation type, entity view types, depth of the search, and other query parameters defined using complex 'EntityViewSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info." + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityViews", method = RequestMethod.POST)
    @ResponseBody
    public List<EntityView> findByQuery(
            @ApiParam(value = "The entity view search query JSON")
            @RequestBody EntityViewSearchQuery query) throws EchoiotException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getEntityViewTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            List<EntityView> entityViews = checkNotNull(entityViewService.findEntityViewsByQuery(getTenantId(), query).get());
            entityViews = entityViews.stream().filter(entityView -> {
                try {
                    accessControlService.checkPermission(getCurrentUser(), PerResource.ENTITY_VIEW, Operation.READ, entityView.getId(), entityView);
                    return true;
                } catch (EchoiotException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            return entityViews;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Entity View Types (getEntityViewTypes)",
            notes = "Returns a set of unique entity view types based on entity views that are either owned by the tenant or assigned to the customer which user is performing the request."
                    + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entityView/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getEntityViewTypes() throws EchoiotException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> entityViewTypes = entityViewService.findEntityViewTypesByTenantId(tenantId);
            return checkNotNull(entityViewTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Make entity view publicly available (assignEntityViewToPublicCustomer)",
            notes = "Entity View will be available for non-authorized (not logged-in) users. " +
                    "This is useful to create dashboards that you plan to share/embed on a publicly available website. " +
                    "However, users that are logged-in and belong to different tenant will not be able to access the entity view." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/entityView/{entityViewId}", method = RequestMethod.POST)
    @ResponseBody
    public EntityView assignEntityViewToPublicCustomer(
            @ApiParam(value = ControllerConstants.ENTITY_VIEW_ID_PARAM_DESCRIPTION)
            @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws EchoiotException {
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);
        EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
        checkEntityViewId(entityViewId, Operation.ASSIGN_TO_CUSTOMER);

        Customer publicCustomer = customerService.findOrCreatePublicCustomer(getTenantId());

        return tbEntityViewService.assignEntityViewToPublicCustomer(getTenantId(), getCurrentUser().getCustomerId(),
                publicCustomer, entityViewId, getCurrentUser());
    }

    @ApiOperation(value = "Assign entity view to edge (assignEntityViewToEdge)",
            notes = "Creates assignment of an existing entity view to an instance of The Edge. " +
                    ControllerConstants.EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive a copy of assignment entity view " +
                    ControllerConstants.EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once entity view will be delivered to edge service, it's going to be available for usage on remote edge instance.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/entityView/{entityViewId}", method = RequestMethod.POST)
    @ResponseBody
    public EntityView assignEntityViewToEdge(@PathVariable(EdgeController.EDGE_ID) String strEdgeId,
                                             @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws EchoiotException {
        checkParameter(EdgeController.EDGE_ID, strEdgeId);
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);

        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
        checkEntityViewId(entityViewId, Operation.READ);

        return tbEntityViewService.assignEntityViewToEdge(getTenantId(), getCurrentUser().getCustomerId(),
                entityViewId, edge, getCurrentUser());
    }

    @ApiOperation(value = "Unassign entity view from edge (unassignEntityViewFromEdge)",
            notes = "Clears assignment of the entity view to the edge. " +
                    ControllerConstants.EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive an 'unassign' command to remove entity view " +
                    ControllerConstants.EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove entity view locally.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/entityView/{entityViewId}", method = RequestMethod.DELETE)
    @ResponseBody
    public EntityView unassignEntityViewFromEdge(@PathVariable(EdgeController.EDGE_ID) String strEdgeId,
                                                 @PathVariable(ENTITY_VIEW_ID) String strEntityViewId) throws EchoiotException {
        checkParameter(EdgeController.EDGE_ID, strEdgeId);
        checkParameter(ENTITY_VIEW_ID, strEntityViewId);

        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        EntityViewId entityViewId = new EntityViewId(toUUID(strEntityViewId));
        EntityView entityView = checkEntityViewId(entityViewId, Operation.READ);

        return tbEntityViewService.unassignEntityViewFromEdge(getTenantId(), entityView.getCustomerId(), entityView,
                edge, getCurrentUser());
    }

    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}/entityViews", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<EntityView> getEdgeEntityViews(
            @PathVariable(EdgeController.EDGE_ID) String strEdgeId,
            @RequestParam int pageSize,
            @RequestParam int page,
            @Nullable @RequestParam(required = false) String type,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String sortProperty,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime) throws EchoiotException {
        checkParameter(EdgeController.EDGE_ID, strEdgeId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            checkEdgeId(edgeId, Operation.READ);
            TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            PageData<EntityView> nonFilteredResult;
            if (type != null && type.trim().length() > 0) {
                nonFilteredResult = entityViewService.findEntityViewsByTenantIdAndEdgeIdAndType(tenantId, edgeId, type, pageLink);
            } else {
                nonFilteredResult = entityViewService.findEntityViewsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            }
            List<EntityView> filteredEntityViews = nonFilteredResult.getData().stream().filter(entityView -> {
                try {
                    accessControlService.checkPermission(getCurrentUser(), PerResource.ENTITY_VIEW, Operation.READ, entityView.getId(), entityView);
                    return true;
                } catch (EchoiotException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            PageData<EntityView> filteredResult = new PageData<>(filteredEntityViews,
                                                                          nonFilteredResult.getTotalPages(),
                                                                          nonFilteredResult.getTotalElements(),
                                                                          nonFilteredResult.hasNext());
            return checkNotNull(filteredResult);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
