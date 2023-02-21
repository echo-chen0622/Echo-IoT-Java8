package org.echoiot.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.TbResource;
import org.echoiot.server.common.data.TbResourceInfo;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.TbResourceId;
import org.echoiot.server.common.data.lwm2m.LwM2mObject;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.resource.TbResourceService;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.PerResource;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;

import static org.echoiot.server.controller.ControllerConstants.*;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class TbResourceController extends BaseController {

    @NotNull
    private final TbResourceService tbResourceService;

    public static final String RESOURCE_ID = "resourceId";

    @NotNull
    @ApiOperation(value = "Download Resource (downloadResource)", notes = "Download Resource based on the provided Resource Id." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource/{resourceId}/download", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<org.springframework.core.io.Resource> downloadResource(@NotNull @ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                                                                                 @PathVariable(RESOURCE_ID) String strResourceId) throws EchoiotException {
        checkParameter(RESOURCE_ID, strResourceId);
        try {
            @NotNull TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
            TbResource tbResource = checkResourceId(resourceId, Operation.READ);

            @NotNull ByteArrayResource resource = new ByteArrayResource(Base64.getDecoder().decode(tbResource.getData().getBytes()));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + tbResource.getFileName())
                    .header("x-filename", tbResource.getFileName())
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Resource Info (getResourceInfoById)",
            notes = "Fetch the Resource Info object based on the provided Resource Id. " +
                    RESOURCE_INFO_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource/info/{resourceId}", method = RequestMethod.GET)
    @ResponseBody
    public TbResourceInfo getResourceInfoById(@NotNull @ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                                              @PathVariable(RESOURCE_ID) String strResourceId) throws EchoiotException {
        checkParameter(RESOURCE_ID, strResourceId);
        try {
            @NotNull TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
            return checkResourceInfoId(resourceId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Resource (getResourceById)",
            notes = "Fetch the Resource object based on the provided Resource Id. " +
                    RESOURCE_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource/{resourceId}", method = RequestMethod.GET)
    @ResponseBody
    public TbResource getResourceById(@NotNull @ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                                      @PathVariable(RESOURCE_ID) String strResourceId) throws EchoiotException {
        checkParameter(RESOURCE_ID, strResourceId);
        try {
            @NotNull TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
            return checkResourceId(resourceId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Resource (saveResource)",
            notes = "Create or update the Resource. When creating the Resource, platform generates Resource id as " + UUID_WIKI_LINK +
                    "The newly created Resource id will be present in the response. " +
                    "Specify existing Resource id to update the Resource. " +
                    "Referencing non-existing Resource Id will cause 'Not Found' error. " +
                    "\n\nResource combination of the title with the key is unique in the scope of tenant. " +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Resource entity." +
                    SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json",
            consumes = "application/json")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource", method = RequestMethod.POST)
    @ResponseBody
    public TbResource saveResource(@NotNull @ApiParam(value = "A JSON value representing the Resource.")
                                   @RequestBody TbResource resource) throws Exception {
        resource.setTenantId(getTenantId());
        checkEntity(resource.getId(), resource, PerResource.TB_RESOURCE);
        return tbResourceService.save(resource, getCurrentUser());
    }

    @ApiOperation(value = "Get Resource Infos (getResources)",
            notes = "Returns a page of Resource Info objects owned by tenant or sysadmin. " +
                    PAGE_DATA_PARAMETERS + RESOURCE_INFO_DESCRIPTION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource", method = RequestMethod.GET)
    @ResponseBody
    public PageData<TbResourceInfo> getResources(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                 @RequestParam int pageSize,
                                                 @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                 @RequestParam int page,
                                                 @ApiParam(value = RESOURCE_TEXT_SEARCH_DESCRIPTION)
                                                 @RequestParam(required = false) String textSearch,
                                                 @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = RESOURCE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                 @RequestParam(required = false) String sortProperty,
                                                 @NotNull @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                 @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (Authority.SYS_ADMIN.equals(getCurrentUser().getAuthority())) {
                return checkNotNull(resourceService.findTenantResourcesByTenantId(getTenantId(), pageLink));
            } else {
                return checkNotNull(resourceService.findAllTenantResourcesByTenantId(getTenantId(), pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get LwM2M Objects (getLwm2mListObjectsPage)",
            notes = "Returns a page of LwM2M objects parsed from Resources with type 'LWM2M_MODEL' owned by tenant or sysadmin. " +
                    PAGE_DATA_PARAMETERS + LWM2M_OBJECT_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/resource/lwm2m/page", method = RequestMethod.GET)
    @ResponseBody
    public List<LwM2mObject> getLwm2mListObjectsPage(@ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
                                                     @RequestParam int pageSize,
                                                     @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
                                                     @RequestParam int page,
                                                     @ApiParam(value = RESOURCE_TEXT_SEARCH_DESCRIPTION)
                                                     @RequestParam(required = false) String textSearch,
                                                     @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = LWM2M_OBJECT_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                     @RequestParam(required = false) String sortProperty,
                                                     @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
                                                     @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            @NotNull PageLink pageLink = new PageLink(pageSize, page, textSearch);
            return checkNotNull(resourceService.findLwM2mObjectPage(getTenantId(), sortProperty, sortOrder, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get LwM2M Objects (getLwm2mListObjects)",
            notes = "Returns a page of LwM2M objects parsed from Resources with type 'LWM2M_MODEL' owned by tenant or sysadmin. " +
                    "You can specify parameters to filter the results. " + LWM2M_OBJECT_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/resource/lwm2m", method = RequestMethod.GET)
    @ResponseBody
    public List<LwM2mObject> getLwm2mListObjects(@ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES, required = true)
                                                 @RequestParam String sortOrder,
                                                 @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = LWM2M_OBJECT_SORT_PROPERTY_ALLOWABLE_VALUES, required = true)
                                                 @RequestParam String sortProperty,
                                                 @ApiParam(value = "LwM2M Object ids.", required = true)
                                                 @RequestParam(required = false) String[] objectIds) throws EchoiotException {
        try {
            return checkNotNull(resourceService.findLwM2mObject(getTenantId(), sortOrder, sortProperty, objectIds));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Delete Resource (deleteResource)",
            notes = "Deletes the Resource. Referencing non-existing Resource Id will cause an error." + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/resource/{resourceId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteResource(@NotNull @ApiParam(value = RESOURCE_ID_PARAM_DESCRIPTION)
                               @PathVariable("resourceId") String strResourceId) throws EchoiotException {
        checkParameter(RESOURCE_ID, strResourceId);
        @NotNull TbResourceId resourceId = new TbResourceId(toUUID(strResourceId));
        TbResource tbResource = checkResourceId(resourceId, Operation.DELETE);
        tbResourceService.delete(tbResource, getCurrentUser());
    }
}
