package org.echoiot.server.controller;

import com.google.common.util.concurrent.ListenableFuture;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.EntitySubtype;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.asset.AssetInfo;
import org.echoiot.server.common.data.asset.AssetSearchQuery;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.page.TimePageLink;
import org.echoiot.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.echoiot.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.echoiot.server.dao.exception.IncorrectParameterException;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.asset.AssetBulkImportService;
import org.echoiot.server.service.entitiy.asset.TbAssetService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AssetController extends BaseController {
    @NotNull
    private final AssetBulkImportService assetBulkImportService;
    @NotNull
    private final TbAssetService tbAssetService;

    public static final String ASSET_ID = "assetId";

    @ApiOperation(value = "Get Asset (getAssetById)",
            notes = "Fetch the Asset object based on the provided Asset Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer." + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/asset/{assetId}", method = RequestMethod.GET)
    @ResponseBody
    public Asset getAssetById(@NotNull @ApiParam(value = ControllerConstants.ASSET_ID_PARAM_DESCRIPTION)
                              @PathVariable(ASSET_ID) String strAssetId) throws EchoiotException {
        checkParameter(ASSET_ID, strAssetId);
        try {
            @NotNull AssetId assetId = new AssetId(toUUID(strAssetId));
            return checkAssetId(assetId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Asset Info (getAssetInfoById)",
            notes = "Fetch the Asset Info object based on the provided Asset Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the asset is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the asset is assigned to the same customer. "
                    + ControllerConstants.ASSET_INFO_DESCRIPTION + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/asset/info/{assetId}", method = RequestMethod.GET)
    @ResponseBody
    public AssetInfo getAssetInfoById(@NotNull @ApiParam(value = ControllerConstants.ASSET_ID_PARAM_DESCRIPTION)
                                      @PathVariable(ASSET_ID) String strAssetId) throws EchoiotException {
        checkParameter(ASSET_ID, strAssetId);
        try {
            @NotNull AssetId assetId = new AssetId(toUUID(strAssetId));
            return checkAssetInfoId(assetId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Asset (saveAsset)",
            notes = "Creates or Updates the Asset. When creating asset, platform generates Asset Id as " + ControllerConstants.UUID_WIKI_LINK +
                    "The newly created Asset id will be present in the response. " +
                    "Specify existing Asset id to update the asset. " +
                    "Referencing non-existing Asset Id will cause 'Not Found' error. " +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Asset entity. "
                    + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/asset", method = RequestMethod.POST)
    @ResponseBody
    public Asset saveAsset(@NotNull @ApiParam(value = "A JSON value representing the asset.") @RequestBody Asset asset) throws Exception {
        asset.setTenantId(getTenantId());
        checkEntity(asset.getId(), asset, Resource.ASSET);
        return tbAssetService.save(asset, getCurrentUser());
    }

    @ApiOperation(value = "Delete asset (deleteAsset)",
            notes = "Deletes the asset and all the relations (from and to the asset). Referencing non-existing asset Id will cause an error." + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/asset/{assetId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAsset(@NotNull @ApiParam(value = ControllerConstants.ASSET_ID_PARAM_DESCRIPTION) @PathVariable(ASSET_ID) String strAssetId) throws Exception {
        checkParameter(ASSET_ID, strAssetId);
        @NotNull AssetId assetId = new AssetId(toUUID(strAssetId));
        Asset asset = checkAssetId(assetId, Operation.DELETE);
        tbAssetService.delete(asset, getCurrentUser()).get();
    }

    @ApiOperation(value = "Assign asset to customer (assignAssetToCustomer)",
            notes = "Creates assignment of the asset to customer. Customer will be able to query asset afterwards." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/asset/{assetId}", method = RequestMethod.POST)
    @ResponseBody
    public Asset assignAssetToCustomer(@NotNull @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION) @PathVariable("customerId") String strCustomerId,
                                       @NotNull @ApiParam(value = ControllerConstants.ASSET_ID_PARAM_DESCRIPTION) @PathVariable(ASSET_ID) String strAssetId) throws EchoiotException {
        checkParameter("customerId", strCustomerId);
        checkParameter(ASSET_ID, strAssetId);
        @NotNull CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        @NotNull AssetId assetId = new AssetId(toUUID(strAssetId));
        checkAssetId(assetId, Operation.ASSIGN_TO_CUSTOMER);
        return tbAssetService.assignAssetToCustomer(getTenantId(), assetId, customer, getCurrentUser());
    }

    @ApiOperation(value = "Unassign asset from customer (unassignAssetFromCustomer)",
            notes = "Clears assignment of the asset to customer. Customer will not be able to query asset afterwards." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/asset/{assetId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Asset unassignAssetFromCustomer(@NotNull @ApiParam(value = ControllerConstants.ASSET_ID_PARAM_DESCRIPTION) @PathVariable(ASSET_ID) String strAssetId) throws EchoiotException {
        checkParameter(ASSET_ID, strAssetId);
        @NotNull AssetId assetId = new AssetId(toUUID(strAssetId));
        Asset asset = checkAssetId(assetId, Operation.UNASSIGN_FROM_CUSTOMER);
        if (asset.getCustomerId() == null || asset.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            throw new IncorrectParameterException("Asset isn't assigned to any customer!");
        }
        Customer customer = checkCustomerId(asset.getCustomerId(), Operation.READ);
        return tbAssetService.unassignAssetToCustomer(getTenantId(), assetId, customer, getCurrentUser());
    }

    @ApiOperation(value = "Make asset publicly available (assignAssetToPublicCustomer)",
            notes = "Asset will be available for non-authorized (not logged-in) users. " +
                    "This is useful to create dashboards that you plan to share/embed on a publicly available website. " +
                    "However, users that are logged-in and belong to different tenant will not be able to access the asset." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/asset/{assetId}", method = RequestMethod.POST)
    @ResponseBody
    public Asset assignAssetToPublicCustomer(@NotNull @ApiParam(value = ControllerConstants.ASSET_ID_PARAM_DESCRIPTION) @PathVariable(ASSET_ID) String strAssetId) throws EchoiotException {
        checkParameter(ASSET_ID, strAssetId);
        @NotNull AssetId assetId = new AssetId(toUUID(strAssetId));
        checkAssetId(assetId, Operation.ASSIGN_TO_CUSTOMER);
        return tbAssetService.assignAssetToPublicCustomer(getTenantId(), assetId, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Assets (getTenantAssets)",
            notes = "Returns a page of assets owned by tenant. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/assets", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Asset> getTenantAssets(
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @Nullable @ApiParam(value = ControllerConstants.ASSET_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = ControllerConstants.ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(assetService.findAssetsByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(assetService.findAssetsByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Asset Infos (getTenantAssetInfos)",
            notes = "Returns a page of assets info objects owned by tenant. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.ASSET_INFO_DESCRIPTION + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/assetInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AssetInfo> getTenantAssetInfos(
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @Nullable @ApiParam(value = ControllerConstants.ASSET_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @Nullable @ApiParam(value = ControllerConstants.ASSET_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(required = false) String assetProfileId,
            @ApiParam(value = ControllerConstants.ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(assetService.findAssetInfosByTenantIdAndType(tenantId, type, pageLink));
            } else if (assetProfileId != null && assetProfileId.length() > 0) {
                @NotNull AssetProfileId profileId = new AssetProfileId(toUUID(assetProfileId));
                return checkNotNull(assetService.findAssetInfosByTenantIdAndAssetProfileId(tenantId, profileId, pageLink));
            } else {
                return checkNotNull(assetService.findAssetInfosByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Asset (getTenantAsset)",
            notes = "Requested asset must be owned by tenant that the user belongs to. " +
                    "Asset name is an unique property of asset. So it can be used to identify the asset." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/assets", params = {"assetName"}, method = RequestMethod.GET)
    @ResponseBody
    public Asset getTenantAsset(
            @ApiParam(value = ControllerConstants.ASSET_NAME_DESCRIPTION)
            @RequestParam String assetName) throws EchoiotException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(assetService.findAssetByTenantIdAndName(tenantId, assetName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Assets (getCustomerAssets)",
            notes = "Returns a page of assets objects assigned to customer. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/assets", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Asset> getCustomerAssets(
            @NotNull @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable("customerId") String strCustomerId,
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @Nullable @ApiParam(value = ControllerConstants.ASSET_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = ControllerConstants.ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            @NotNull CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Asset Infos (getCustomerAssetInfos)",
            notes = "Returns a page of assets info objects assigned to customer. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.ASSET_INFO_DESCRIPTION, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/assetInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AssetInfo> getCustomerAssetInfos(
            @NotNull @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION)
            @PathVariable("customerId") String strCustomerId,
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @Nullable @ApiParam(value = ControllerConstants.ASSET_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @Nullable @ApiParam(value = ControllerConstants.ASSET_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(required = false) String assetProfileId,
            @ApiParam(value = ControllerConstants.ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        checkParameter("customerId", strCustomerId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            @NotNull CustomerId customerId = new CustomerId(toUUID(strCustomerId));
            checkCustomerId(customerId, Operation.READ);
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(assetService.findAssetInfosByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else if (assetProfileId != null && assetProfileId.length() > 0) {
                @NotNull AssetProfileId profileId = new AssetProfileId(toUUID(assetProfileId));
                return checkNotNull(assetService.findAssetInfosByTenantIdAndCustomerIdAndAssetProfileId(tenantId, customerId, profileId, pageLink));
            } else {
                return checkNotNull(assetService.findAssetInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Assets By Ids (getAssetsByIds)",
            notes = "Requested assets must be owned by tenant or assigned to customer which user is performing the request. ", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/assets", params = {"assetIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Asset> getAssetsByIds(
            @NotNull @ApiParam(value = "A list of assets ids, separated by comma ','")
            @RequestParam("assetIds") String[] strAssetIds) throws EchoiotException {
        checkArrayParameter("assetIds", strAssetIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            CustomerId customerId = user.getCustomerId();
            @NotNull List<AssetId> assetIds = new ArrayList<>();
            for (@NotNull String strAssetId : strAssetIds) {
                assetIds.add(new AssetId(toUUID(strAssetId)));
            }
            ListenableFuture<List<Asset>> assets;
            if (customerId == null || customerId.isNullUid()) {
                assets = assetService.findAssetsByTenantIdAndIdsAsync(tenantId, assetIds);
            } else {
                assets = assetService.findAssetsByTenantIdCustomerIdAndIdsAsync(tenantId, customerId, assetIds);
            }
            return checkNotNull(assets.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @NotNull
    @ApiOperation(value = "Find related assets (findByQuery)",
            notes = "Returns all assets that are related to the specific entity. " +
                    "The entity id, relation type, asset types, depth of the search, and other query parameters defined using complex 'AssetSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/assets", method = RequestMethod.POST)
    @ResponseBody
    public List<Asset> findByQuery(@NotNull @RequestBody AssetSearchQuery query) throws EchoiotException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getAssetTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            List<Asset> assets = checkNotNull(assetService.findAssetsByQuery(getTenantId(), query).get());
            assets = assets.stream().filter(asset -> {
                try {
                    accessControlService.checkPermission(getCurrentUser(), Resource.ASSET, Operation.READ, asset.getId(), asset);
                    return true;
                } catch (EchoiotException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            return assets;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Asset Types (getAssetTypes)",
            notes = "Returns a set of unique asset types based on assets that are either owned by the tenant or assigned to the customer which user is performing the request.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/asset/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getAssetTypes() throws EchoiotException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> assetTypes = assetService.findAssetTypesByTenantId(tenantId);
            return checkNotNull(assetTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Assign asset to edge (assignAssetToEdge)",
            notes = "Creates assignment of an existing asset to an instance of The Edge. " +
                    ControllerConstants.EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive a copy of assignment asset " +
                    ControllerConstants.EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once asset will be delivered to edge service, it's going to be available for usage on remote edge instance.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/asset/{assetId}", method = RequestMethod.POST)
    @ResponseBody
    public Asset assignAssetToEdge(@NotNull @ApiParam(value = ControllerConstants.EDGE_ID_PARAM_DESCRIPTION) @PathVariable(EdgeController.EDGE_ID) String strEdgeId,
                                   @NotNull @ApiParam(value = ControllerConstants.ASSET_ID_PARAM_DESCRIPTION) @PathVariable(ASSET_ID) String strAssetId) throws EchoiotException {
        checkParameter(EdgeController.EDGE_ID, strEdgeId);
        checkParameter(ASSET_ID, strAssetId);

        @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        @NotNull AssetId assetId = new AssetId(toUUID(strAssetId));
        checkAssetId(assetId, Operation.READ);

        return tbAssetService.assignAssetToEdge(getTenantId(), assetId, edge, getCurrentUser());
    }

    @ApiOperation(value = "Unassign asset from edge (unassignAssetFromEdge)",
            notes = "Clears assignment of the asset to the edge. " +
                    ControllerConstants.EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive an 'unassign' command to remove asset " +
                    ControllerConstants.EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove asset locally.",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/asset/{assetId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Asset unassignAssetFromEdge(@NotNull @ApiParam(value = ControllerConstants.EDGE_ID_PARAM_DESCRIPTION) @PathVariable(EdgeController.EDGE_ID) String strEdgeId,
                                       @NotNull @ApiParam(value = ControllerConstants.ASSET_ID_PARAM_DESCRIPTION) @PathVariable(ASSET_ID) String strAssetId) throws EchoiotException {
        checkParameter(EdgeController.EDGE_ID, strEdgeId);
        checkParameter(ASSET_ID, strAssetId);
        @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        @NotNull AssetId assetId = new AssetId(toUUID(strAssetId));
        Asset asset = checkAssetId(assetId, Operation.READ);

        return tbAssetService.unassignAssetFromEdge(getTenantId(), asset, edge, getCurrentUser());
    }

    @ApiOperation(value = "Get assets assigned to edge (getEdgeAssets)",
            notes = "Returns a page of assets assigned to edge. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}/assets", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Asset> getEdgeAssets(
            @NotNull @ApiParam(value = ControllerConstants.EDGE_ID_PARAM_DESCRIPTION)
            @PathVariable(EdgeController.EDGE_ID) String strEdgeId,
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION)
            @RequestParam int page,
            @Nullable @ApiParam(value = ControllerConstants.ASSET_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = ControllerConstants.ASSET_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.ASSET_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = "Timestamp. Assets with creation time before it won't be queried")
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = "Timestamp. Assets with creation time after it won't be queried")
            @RequestParam(required = false) Long endTime) throws EchoiotException {
        checkParameter(EdgeController.EDGE_ID, strEdgeId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            checkEdgeId(edgeId, Operation.READ);
            @NotNull TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            PageData<Asset> nonFilteredResult;
            if (type != null && type.trim().length() > 0) {
                nonFilteredResult = assetService.findAssetsByTenantIdAndEdgeIdAndType(tenantId, edgeId, type, pageLink);
            } else {
                nonFilteredResult = assetService.findAssetsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            }
            @NotNull List<Asset> filteredAssets = nonFilteredResult.getData().stream().filter(asset -> {
                try {
                    accessControlService.checkPermission(getCurrentUser(), Resource.ASSET, Operation.READ, asset.getId(), asset);
                    return true;
                } catch (EchoiotException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            @NotNull PageData<Asset> filteredResult = new PageData<>(filteredAssets,
                                                                     nonFilteredResult.getTotalPages(),
                                                                     nonFilteredResult.getTotalElements(),
                                                                     nonFilteredResult.hasNext());
            return checkNotNull(filteredResult);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @NotNull
    @ApiOperation(value = "Import the bulk of assets (processAssetsBulkImport)",
            notes = "There's an ability to import the bulk of assets using the only .csv file.", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping("/asset/bulk_import")
    public BulkImportResult<Asset> processAssetsBulkImport(@NotNull @RequestBody BulkImportRequest request) throws Exception {
        SecurityUser user = getCurrentUser();
        return assetBulkImportService.processBulkImport(request, user);
    }

}
