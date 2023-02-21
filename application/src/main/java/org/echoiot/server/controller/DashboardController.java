package org.echoiot.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DashboardId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.dashboard.TbDashboardService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class DashboardController extends BaseController {

    @NotNull
    private final TbDashboardService tbDashboardService;
    public static final String DASHBOARD_ID = "dashboardId";

    private static final String HOME_DASHBOARD_ID = "homeDashboardId";
    private static final String HOME_DASHBOARD_HIDE_TOOLBAR = "homeDashboardHideToolbar";
    public static final String DASHBOARD_INFO_DEFINITION = "The Dashboard Info object contains lightweight information about the dashboard (e.g. title, image, assigned customers) but does not contain the heavyweight configuration JSON.";
    public static final String DASHBOARD_DEFINITION = "The Dashboard object is a heavyweight object that contains information about the dashboard (e.g. title, image, assigned customers) and also configuration JSON (e.g. layouts, widgets, entity aliases).";
    public static final String HIDDEN_FOR_MOBILE = "Exclude dashboards that are hidden for mobile";

    @Value("${ui.dashboard.max_datapoints_limit}")
    private long maxDatapointsLimit;

    @ApiOperation(value = "Get server time (getServerTime)",
            notes = "Get the server time (milliseconds since January 1, 1970 UTC). " +
                    "Used to adjust view of the dashboards according to the difference between browser and server time.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/serverTime", method = RequestMethod.GET)
    @ResponseBody
    @ApiResponse(code = 200, message = "OK", examples = @Example(value = @ExampleProperty(value = "1636023857137", mediaType = "application/json")))
    public long getServerTime() throws EchoiotException {
        return System.currentTimeMillis();
    }

    @ApiOperation(value = "Get max data points limit (getMaxDatapointsLimit)",
            notes = "Get the maximum number of data points that dashboard may request from the server per in a single subscription command. " +
                    "This value impacts the time window behavior. It impacts 'Max values' parameter in case user selects 'None' as 'Data aggregation function'. " +
                    "It also impacts the 'Grouping interval' in case of any other 'Data aggregation function' is selected. " +
                    "The actual value of the limit is configurable in the system configuration file.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/maxDatapointsLimit", method = RequestMethod.GET)
    @ResponseBody
    @ApiResponse(code = 200, message = "OK", examples = @Example(value = @ExampleProperty(value = "5000", mediaType = "application/json")))
    public long getMaxDatapointsLimit() throws EchoiotException {
        return maxDatapointsLimit;
    }

    @ApiOperation(value = "Get Dashboard Info (getDashboardInfoById)",
            notes = "Get the information about the dashboard based on 'dashboardId' parameter. " + DASHBOARD_INFO_DEFINITION,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/info/{dashboardId}", method = RequestMethod.GET)
    @ResponseBody
    public DashboardInfo getDashboardInfoById(
            @NotNull @ApiParam(value = ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws EchoiotException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            @NotNull DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            return checkDashboardInfoId(dashboardId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Dashboard (getDashboardById)",
            notes = "Get the dashboard based on 'dashboardId' parameter. " + DASHBOARD_DEFINITION + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/{dashboardId}", method = RequestMethod.GET)
    @ResponseBody
    public Dashboard getDashboardById(
            @NotNull @ApiParam(value = ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws EchoiotException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        try {
            @NotNull DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
            return checkDashboardId(dashboardId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Dashboard (saveDashboard)",
            notes = "Create or update the Dashboard. When creating dashboard, platform generates Dashboard Id as " + ControllerConstants.UUID_WIKI_LINK +
                    "The newly created Dashboard id will be present in the response. " +
                    "Specify existing Dashboard id to update the dashboard. " +
                    "Referencing non-existing dashboard Id will cause 'Not Found' error. " +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Dashboard entity. " +
                    ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/dashboard", method = RequestMethod.POST)
    @ResponseBody
    public Dashboard saveDashboard(
            @NotNull @ApiParam(value = "A JSON value representing the dashboard.")
            @RequestBody Dashboard dashboard) throws Exception {
        dashboard.setTenantId(getTenantId());
        checkEntity(dashboard.getId(), dashboard, Resource.DASHBOARD);
        return tbDashboardService.save(dashboard, getCurrentUser());
    }

    @ApiOperation(value = "Delete the Dashboard (deleteDashboard)",
            notes = "Delete the Dashboard." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDashboard(
            @NotNull @ApiParam(value = ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws EchoiotException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        @NotNull DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.DELETE);
        tbDashboardService.delete(dashboard, getCurrentUser());
    }

    @ApiOperation(value = "Assign the Dashboard (assignDashboardToCustomer)",
            notes = "Assign the Dashboard to specified Customer or do nothing if the Dashboard is already assigned to that Customer. " +
                    "Returns the Dashboard object." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/dashboard/{dashboardId}", method = RequestMethod.POST)
    @ResponseBody
    public Dashboard assignDashboardToCustomer(
            @NotNull @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(ControllerConstants.CUSTOMER_ID) String strCustomerId,
            @NotNull @ApiParam(value = ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws EchoiotException {
        checkParameter(ControllerConstants.CUSTOMER_ID, strCustomerId);
        checkParameter(DASHBOARD_ID, strDashboardId);

        @NotNull CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);

        @NotNull DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.ASSIGN_TO_CUSTOMER);
        return tbDashboardService.assignDashboardToCustomer(dashboard, customer, getCurrentUser());
    }

    @ApiOperation(value = "Unassign the Dashboard (unassignDashboardFromCustomer)",
            notes = "Unassign the Dashboard from specified Customer or do nothing if the Dashboard is already assigned to that Customer. " +
                    "Returns the Dashboard object." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Dashboard unassignDashboardFromCustomer(
            @NotNull @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable(ControllerConstants.CUSTOMER_ID) String strCustomerId,
            @NotNull @ApiParam(value = ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws EchoiotException {
        checkParameter("customerId", strCustomerId);
        checkParameter(DASHBOARD_ID, strDashboardId);
        @NotNull CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        @NotNull DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.UNASSIGN_FROM_CUSTOMER);
        return tbDashboardService.unassignDashboardFromCustomer(dashboard, customer, getCurrentUser());
    }

    @ApiOperation(value = "Update the Dashboard Customers (updateDashboardCustomers)",
            notes = "Updates the list of Customers that this Dashboard is assigned to. Removes previous assignments to customers that are not in the provided list. " +
                    "Returns the Dashboard object. " + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/dashboard/{dashboardId}/customers", method = RequestMethod.POST)
    @ResponseBody
    public Dashboard updateDashboardCustomers(
            @NotNull @ApiParam(value = ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId,
            @ApiParam(value = "JSON array with the list of customer ids, or empty to remove all customers")
            @RequestBody(required = false) String[] strCustomerIds) throws EchoiotException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        @NotNull DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.ASSIGN_TO_CUSTOMER);
        @NotNull Set<CustomerId> customerIds = customerIdFromStr(strCustomerIds);
        return tbDashboardService.updateDashboardCustomers(dashboard, customerIds, getCurrentUser());
    }

    @ApiOperation(value = "Adds the Dashboard Customers (addDashboardCustomers)",
            notes = "Adds the list of Customers to the existing list of assignments for the Dashboard. Keeps previous assignments to customers that are not in the provided list. " +
                    "Returns the Dashboard object." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/dashboard/{dashboardId}/customers/add", method = RequestMethod.POST)
    @ResponseBody
    public Dashboard addDashboardCustomers(
            @NotNull @ApiParam(value = ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId,
            @ApiParam(value = "JSON array with the list of customer ids")
            @RequestBody String[] strCustomerIds) throws EchoiotException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        @NotNull DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.ASSIGN_TO_CUSTOMER);
        @NotNull Set<CustomerId> customerIds = customerIdFromStr(strCustomerIds);
        return tbDashboardService.addDashboardCustomers(dashboard, customerIds, getCurrentUser());
    }

    @ApiOperation(value = "Remove the Dashboard Customers (removeDashboardCustomers)",
            notes = "Removes the list of Customers from the existing list of assignments for the Dashboard. Keeps other assignments to customers that are not in the provided list. " +
                    "Returns the Dashboard object." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/dashboard/{dashboardId}/customers/remove", method = RequestMethod.POST)
    @ResponseBody
    public Dashboard removeDashboardCustomers(
            @NotNull @ApiParam(value = ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId,
            @ApiParam(value = "JSON array with the list of customer ids")
            @RequestBody String[] strCustomerIds) throws EchoiotException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        @NotNull DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.UNASSIGN_FROM_CUSTOMER);
        @NotNull Set<CustomerId> customerIds = customerIdFromStr(strCustomerIds);
        return tbDashboardService.removeDashboardCustomers(dashboard, customerIds, getCurrentUser());
    }

    @ApiOperation(value = "Assign the Dashboard to Public Customer (assignDashboardToPublicCustomer)",
            notes = "Assigns the dashboard to a special, auto-generated 'Public' Customer. Once assigned, unauthenticated users may browse the dashboard. " +
                    "This method is useful if you like to embed the dashboard on public web pages to be available for users that are not logged in. " +
                    "Be aware that making the dashboard public does not mean that it automatically makes all devices and assets you use in the dashboard to be public." +
                    "Use [assign Asset to Public Customer](#!/asset-controller/assignAssetToPublicCustomerUsingPOST) and " +
                    "[assign Device to Public Customer](#!/device-controller/assignDeviceToPublicCustomerUsingPOST) for this purpose. " +
                    "Returns the Dashboard object." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/dashboard/{dashboardId}", method = RequestMethod.POST)
    @ResponseBody
    public Dashboard assignDashboardToPublicCustomer(
            @NotNull @ApiParam(value = ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws EchoiotException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        @NotNull DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.ASSIGN_TO_CUSTOMER);
        return tbDashboardService.assignDashboardToPublicCustomer(dashboard, getCurrentUser());
    }

    @ApiOperation(value = "Unassign the Dashboard from Public Customer (unassignDashboardFromPublicCustomer)",
            notes = "Unassigns the dashboard from a special, auto-generated 'Public' Customer. Once unassigned, unauthenticated users may no longer browse the dashboard. " +
                    "Returns the Dashboard object." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Dashboard unassignDashboardFromPublicCustomer(
            @NotNull @ApiParam(value = ControllerConstants.DASHBOARD_ID_PARAM_DESCRIPTION)
            @PathVariable(DASHBOARD_ID) String strDashboardId) throws EchoiotException {
        checkParameter(DASHBOARD_ID, strDashboardId);
        @NotNull DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.UNASSIGN_FROM_CUSTOMER);
        return tbDashboardService.unassignDashboardFromPublicCustomer(dashboard, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Dashboards by System Administrator (getTenantDashboards)",
            notes = "Returns a page of dashboard info objects owned by tenant. " + DASHBOARD_INFO_DEFINITION + " " + ControllerConstants.PAGE_DATA_PARAMETERS +
                    ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getTenantDashboards(
            @NotNull @ApiParam(value = ControllerConstants.TENANT_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ControllerConstants.TENANT_ID) String strTenantId,
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ControllerConstants.DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            @NotNull TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
            checkTenantId(tenantId, Operation.READ);
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Dashboards (getTenantDashboards)",
            notes = "Returns a page of dashboard info objects owned by the tenant of a current user. "
                    + DASHBOARD_INFO_DEFINITION + " " + ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getTenantDashboards(
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Nullable @ApiParam(value = HIDDEN_FOR_MOBILE)
            @RequestParam(required = false) Boolean mobile,
            @ApiParam(value = ControllerConstants.DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (mobile != null && mobile) {
                return checkNotNull(dashboardService.findMobileDashboardsByTenantId(tenantId, pageLink));
            } else {
                return checkNotNull(dashboardService.findDashboardsByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Dashboards (getCustomerDashboards)",
            notes = "Returns a page of dashboard info objects owned by the specified customer. "
                    + DASHBOARD_INFO_DEFINITION + " " + ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getCustomerDashboards(
            @NotNull @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ControllerConstants.CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @Nullable @ApiParam(value = HIDDEN_FOR_MOBILE)
            @RequestParam(required = false) Boolean mobile,
            @ApiParam(value = ControllerConstants.DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        checkParameter(ControllerConstants.CUSTOMER_ID, strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            @NotNull CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (mobile != null && mobile) {
                return checkNotNull(dashboardService.findMobileDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            } else {
                return checkNotNull(dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Nullable
    @ApiOperation(value = "Get Home Dashboard (getHomeDashboard)",
            notes = "Returns the home dashboard object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the User. " +
                    "If 'homeDashboardId' parameter is not set on the User level and the User has authority 'CUSTOMER_USER', check the same parameter for the corresponding Customer. " +
                    "If 'homeDashboardId' parameter is not set on the User and Customer levels then checks the same parameter for the Tenant that owns the user. "
                    + DASHBOARD_DEFINITION + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/home", method = RequestMethod.GET)
    @ResponseBody
    public HomeDashboard getHomeDashboard() throws EchoiotException {
        try {
            SecurityUser securityUser = getCurrentUser();
            if (securityUser.isSystemAdmin()) {
                return null;
            }
            User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
            JsonNode additionalInfo = user.getAdditionalInfo();
            @Nullable HomeDashboard homeDashboard;
            homeDashboard = extractHomeDashboardFromAdditionalInfo(additionalInfo);
            if (homeDashboard == null) {
                if (securityUser.isCustomerUser()) {
                    Customer customer = customerService.findCustomerById(securityUser.getTenantId(), securityUser.getCustomerId());
                    additionalInfo = customer.getAdditionalInfo();
                    homeDashboard = extractHomeDashboardFromAdditionalInfo(additionalInfo);
                }
                if (homeDashboard == null) {
                    Tenant tenant = tenantService.findTenantById(securityUser.getTenantId());
                    additionalInfo = tenant.getAdditionalInfo();
                    homeDashboard = extractHomeDashboardFromAdditionalInfo(additionalInfo);
                }
            }
            return homeDashboard;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Nullable
    @ApiOperation(value = "Get Home Dashboard Info (getHomeDashboardInfo)",
            notes = "Returns the home dashboard info object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the User. " +
                    "If 'homeDashboardId' parameter is not set on the User level and the User has authority 'CUSTOMER_USER', check the same parameter for the corresponding Customer. " +
                    "If 'homeDashboardId' parameter is not set on the User and Customer levels then checks the same parameter for the Tenant that owns the user. " +
                    ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/dashboard/home/info", method = RequestMethod.GET)
    @ResponseBody
    public HomeDashboardInfo getHomeDashboardInfo() throws EchoiotException {
        try {
            SecurityUser securityUser = getCurrentUser();
            if (securityUser.isSystemAdmin()) {
                return null;
            }
            User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
            JsonNode additionalInfo = user.getAdditionalInfo();
            @Nullable HomeDashboardInfo homeDashboardInfo;
            homeDashboardInfo = extractHomeDashboardInfoFromAdditionalInfo(additionalInfo);
            if (homeDashboardInfo == null) {
                if (securityUser.isCustomerUser()) {
                    Customer customer = customerService.findCustomerById(securityUser.getTenantId(), securityUser.getCustomerId());
                    additionalInfo = customer.getAdditionalInfo();
                    homeDashboardInfo = extractHomeDashboardInfoFromAdditionalInfo(additionalInfo);
                }
                if (homeDashboardInfo == null) {
                    Tenant tenant = tenantService.findTenantById(securityUser.getTenantId());
                    additionalInfo = tenant.getAdditionalInfo();
                    homeDashboardInfo = extractHomeDashboardInfoFromAdditionalInfo(additionalInfo);
                }
            }
            return homeDashboardInfo;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @NotNull
    @ApiOperation(value = "Get Tenant Home Dashboard Info (getTenantHomeDashboardInfo)",
            notes = "Returns the home dashboard info object that is configured as 'homeDashboardId' parameter in the 'additionalInfo' of the corresponding tenant. " +
                    ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboard/home/info", method = RequestMethod.GET)
    @ResponseBody
    public HomeDashboardInfo getTenantHomeDashboardInfo() throws EchoiotException {
        try {
            Tenant tenant = tenantService.findTenantById(getTenantId());
            JsonNode additionalInfo = tenant.getAdditionalInfo();
            @Nullable DashboardId dashboardId = null;
            boolean hideDashboardToolbar = true;
            if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID) && !additionalInfo.get(HOME_DASHBOARD_ID).isNull()) {
                String strDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
                dashboardId = new DashboardId(toUUID(strDashboardId));
                if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                    hideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
                }
            }
            return new HomeDashboardInfo(dashboardId, hideDashboardToolbar);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Update Tenant Home Dashboard Info (getTenantHomeDashboardInfo)",
            notes = "Update the home dashboard assignment for the current tenant. " +
                    ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/dashboard/home/info", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void setTenantHomeDashboardInfo(
            @NotNull @ApiParam(value = "A JSON object that represents home dashboard id and other parameters", required = true)
            @RequestBody HomeDashboardInfo homeDashboardInfo) throws EchoiotException {

        try {
            if (homeDashboardInfo.getDashboardId() != null) {
                checkDashboardId(homeDashboardInfo.getDashboardId(), Operation.READ);
            }
            Tenant tenant = tenantService.findTenantById(getTenantId());
            JsonNode additionalInfo = tenant.getAdditionalInfo();
            if (additionalInfo == null || !(additionalInfo instanceof ObjectNode)) {
                additionalInfo = JacksonUtil.OBJECT_MAPPER.createObjectNode();
            }
            if (homeDashboardInfo.getDashboardId() != null) {
                ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_ID, homeDashboardInfo.getDashboardId().getId().toString());
                ((ObjectNode) additionalInfo).put(HOME_DASHBOARD_HIDE_TOOLBAR, homeDashboardInfo.isHideDashboardToolbar());
            } else {
                ((ObjectNode) additionalInfo).remove(HOME_DASHBOARD_ID);
                ((ObjectNode) additionalInfo).remove(HOME_DASHBOARD_HIDE_TOOLBAR);
            }
            tenant.setAdditionalInfo(additionalInfo);
            tenantService.saveTenant(tenant);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Nullable
    private HomeDashboardInfo extractHomeDashboardInfoFromAdditionalInfo(@Nullable JsonNode additionalInfo) {
        try {
            if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID) && !additionalInfo.get(HOME_DASHBOARD_ID).isNull()) {
                String strDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
                @NotNull DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
                checkDashboardId(dashboardId, Operation.READ);
                boolean hideDashboardToolbar = true;
                if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                    hideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
                }
                return new HomeDashboardInfo(dashboardId, hideDashboardToolbar);
            }
        } catch (Exception e) {
        }
        return null;
    }

    @Nullable
    private HomeDashboard extractHomeDashboardFromAdditionalInfo(@Nullable JsonNode additionalInfo) {
        try {
            if (additionalInfo != null && additionalInfo.has(HOME_DASHBOARD_ID) && !additionalInfo.get(HOME_DASHBOARD_ID).isNull()) {
                String strDashboardId = additionalInfo.get(HOME_DASHBOARD_ID).asText();
                @NotNull DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
                Dashboard dashboard = checkDashboardId(dashboardId, Operation.READ);
                boolean hideDashboardToolbar = true;
                if (additionalInfo.has(HOME_DASHBOARD_HIDE_TOOLBAR)) {
                    hideDashboardToolbar = additionalInfo.get(HOME_DASHBOARD_HIDE_TOOLBAR).asBoolean();
                }
                return new HomeDashboard(dashboard, hideDashboardToolbar);
            }
        } catch (Exception e) {
        }
        return null;
    }

    @ApiOperation(value = "Assign dashboard to edge (assignDashboardToEdge)",
            notes = "Creates assignment of an existing dashboard to an instance of The Edge. " +
                    ControllerConstants.EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive a copy of assignment dashboard " +
                    ControllerConstants.EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once dashboard will be delivered to edge service, it's going to be available for usage on remote edge instance." +
                    ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/dashboard/{dashboardId}", method = RequestMethod.POST)
    @ResponseBody
    public Dashboard assignDashboardToEdge(@NotNull @PathVariable("edgeId") String strEdgeId,
                                           @NotNull @PathVariable(DASHBOARD_ID) String strDashboardId) throws EchoiotException {
        checkParameter("edgeId", strEdgeId);
        checkParameter(DASHBOARD_ID, strDashboardId);

        @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        @NotNull DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        checkDashboardId(dashboardId, Operation.READ);
        return tbDashboardService.asignDashboardToEdge(getTenantId(), dashboardId, edge, getCurrentUser());
    }

    @ApiOperation(value = "Unassign dashboard from edge (unassignDashboardFromEdge)",
            notes = "Clears assignment of the dashboard to the edge. " +
                    ControllerConstants.EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive an 'unassign' command to remove dashboard " +
                    ControllerConstants.EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove dashboard locally." +
                    ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/dashboard/{dashboardId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Dashboard unassignDashboardFromEdge(@NotNull @PathVariable("edgeId") String strEdgeId,
                                               @NotNull @PathVariable(DASHBOARD_ID) String strDashboardId) throws EchoiotException {
        checkParameter(ControllerConstants.EDGE_ID, strEdgeId);
        checkParameter(DASHBOARD_ID, strDashboardId);

        @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        @NotNull DashboardId dashboardId = new DashboardId(toUUID(strDashboardId));
        Dashboard dashboard = checkDashboardId(dashboardId, Operation.READ);

        return tbDashboardService.unassignDashboardFromEdge(dashboard, edge, getCurrentUser());
    }

    @ApiOperation(value = "Get Edge Dashboards (getEdgeDashboards)",
            notes = "Returns a page of dashboard info objects assigned to the specified edge. "
                    + DASHBOARD_INFO_DEFINITION + " " + ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}/dashboards", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DashboardInfo> getEdgeDashboards(
            @NotNull @ApiParam(value = ControllerConstants.EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ControllerConstants.EDGE_ID) String strEdgeId,
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ControllerConstants.DASHBOARD_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.DASHBOARD_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        checkParameter("edgeId", strEdgeId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            checkEdgeId(edgeId, Operation.READ);
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            PageData<DashboardInfo> nonFilteredResult = dashboardService.findDashboardsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            @NotNull List<DashboardInfo> filteredDashboards = nonFilteredResult.getData().stream().filter(dashboardInfo -> {
                try {
                    accessControlService.checkPermission(getCurrentUser(), Resource.DASHBOARD, Operation.READ, dashboardInfo.getId(), dashboardInfo);
                    return true;
                } catch (EchoiotException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            @NotNull PageData<DashboardInfo> filteredResult = new PageData<>(filteredDashboards,
                                                                             nonFilteredResult.getTotalPages(),
                                                                             nonFilteredResult.getTotalElements(),
                                                                             nonFilteredResult.hasNext());
            return checkNotNull(filteredResult);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @NotNull
    private Set<CustomerId> customerIdFromStr(@Nullable String[] strCustomerIds) {
        @NotNull Set<CustomerId> customerIds = new HashSet<>();
        if (strCustomerIds != null) {
            for (@NotNull String strCustomerId : strCustomerIds) {
                customerIds.add(new CustomerId(UUID.fromString(strCustomerId)));
            }
        }
        return customerIds;
    }
}
