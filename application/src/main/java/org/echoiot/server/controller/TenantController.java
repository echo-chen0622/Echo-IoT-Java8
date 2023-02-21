package org.echoiot.server.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.TenantInfo;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.tenant.TbTenantService;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.PerResource;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.echoiot.server.controller.ControllerConstants.*;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class TenantController extends BaseController {

    private static final String TENANT_INFO_DESCRIPTION = "The Tenant Info object extends regular Tenant object and includes Tenant Profile name. ";

    @NotNull
    private final TenantService tenantService;
    @NotNull
    private final TbTenantService tbTenantService;

    @NotNull
    @ApiOperation(value = "Get Tenant (getTenantById)",
            notes = "Fetch the Tenant object based on the provided Tenant Id. " + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}", method = RequestMethod.GET)
    @ResponseBody
    public Tenant getTenantById(
            @NotNull @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION)
            @PathVariable(TENANT_ID) String strTenantId) throws EchoiotException {
        checkParameter(TENANT_ID, strTenantId);
        try {
            @NotNull TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
            Tenant tenant = checkTenantId(tenantId, Operation.READ);
            if (!tenant.getAdditionalInfo().isNull()) {
                processDashboardIdFromAdditionalInfo((ObjectNode) tenant.getAdditionalInfo(), HOME_DASHBOARD);
            }
            return tenant;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Info (getTenantInfoById)",
            notes = "Fetch the Tenant Info object based on the provided Tenant Id. " +
                    TENANT_INFO_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/info/{tenantId}", method = RequestMethod.GET)
    @ResponseBody
    public TenantInfo getTenantInfoById(
            @NotNull @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION)
            @PathVariable(TENANT_ID) String strTenantId) throws EchoiotException {
        checkParameter(TENANT_ID, strTenantId);
        try {
            @NotNull TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
            return checkTenantInfoId(tenantId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or update Tenant (saveTenant)",
            notes = "Create or update the Tenant. When creating tenant, platform generates Tenant Id as " + UUID_WIKI_LINK +
                    "Default Rule Chain and Device profile are also generated for the new tenants automatically. " +
                    "The newly created Tenant Id will be present in the response. " +
                    "Specify existing Tenant Id id to update the Tenant. " +
                    "Referencing non-existing Tenant Id will cause 'Not Found' error." +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Tenant entity." +
                    SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant", method = RequestMethod.POST)
    @ResponseBody
    public Tenant saveTenant(@NotNull @ApiParam(value = "A JSON value representing the tenant.")
                             @RequestBody Tenant tenant) throws Exception {
        checkEntity(tenant.getId(), tenant, PerResource.TENANT);
        return tbTenantService.save(tenant);
    }

    @ApiOperation(value = "Delete Tenant (deleteTenant)",
            notes = "Deletes the tenant, it's customers, rule chains, devices and all other related entities. Referencing non-existing tenant Id will cause an error." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteTenant(@NotNull @ApiParam(value = TENANT_ID_PARAM_DESCRIPTION)
                             @PathVariable(TENANT_ID) String strTenantId) throws Exception {
        checkParameter(TENANT_ID, strTenantId);
        @NotNull TenantId tenantId = TenantId.fromUUID(toUUID(strTenantId));
        Tenant tenant = checkTenantId(tenantId, Operation.DELETE);
        tbTenantService.delete(tenant);
    }

    @ApiOperation(value = "Get Tenants (getTenants)", notes = "Returns a page of tenants registered in the platform. " + PAGE_DATA_PARAMETERS + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenants", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Tenant> getTenants(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = TENANT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = TENANT_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(tenantService.findTenants(pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenants Info (getTenants)", notes = "Returns a page of tenant info objects registered in the platform. "
            + TENANT_INFO_DESCRIPTION + PAGE_DATA_PARAMETERS + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<TenantInfo> getTenantInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = TENANT_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = TENANT_INFO_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder
    ) throws EchoiotException {
        try {
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(tenantService.findTenantInfos(pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
