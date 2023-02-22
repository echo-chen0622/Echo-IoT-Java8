package org.echoiot.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.asset.AssetProfileInfo;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.AssetProfileId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.asset.profile.TbAssetProfileService;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.PerResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AssetProfileController extends BaseController {

    private final TbAssetProfileService tbAssetProfileService;

    @ApiOperation(value = "Get Asset Profile (getAssetProfileById)",
            notes = "Fetch the Asset Profile object based on the provided Asset Profile Id. " +
                    "The server checks that the asset profile is owned by the same tenant. " + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/assetProfile/{assetProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public AssetProfile getAssetProfileById(
            @ApiParam(value = ControllerConstants.ASSET_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(ControllerConstants.ASSET_PROFILE_ID) String strAssetProfileId) throws EchoiotException {
        checkParameter(ControllerConstants.ASSET_PROFILE_ID, strAssetProfileId);
        try {
            AssetProfileId assetProfileId = new AssetProfileId(toUUID(strAssetProfileId));
            return checkAssetProfileId(assetProfileId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Asset Profile Info (getAssetProfileInfoById)",
            notes = "Fetch the Asset Profile Info object based on the provided Asset Profile Id. "
                    + ControllerConstants.ASSET_PROFILE_INFO_DESCRIPTION + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/assetProfileInfo/{assetProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public AssetProfileInfo getAssetProfileInfoById(
            @ApiParam(value = ControllerConstants.ASSET_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(ControllerConstants.ASSET_PROFILE_ID) String strAssetProfileId) throws EchoiotException {
        checkParameter(ControllerConstants.ASSET_PROFILE_ID, strAssetProfileId);
        try {
            AssetProfileId assetProfileId = new AssetProfileId(toUUID(strAssetProfileId));
            return new AssetProfileInfo(checkAssetProfileId(assetProfileId, Operation.READ));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Default Asset Profile (getDefaultAssetProfileInfo)",
            notes = "Fetch the Default Asset Profile Info object. " +
                    ControllerConstants.ASSET_PROFILE_INFO_DESCRIPTION + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/assetProfileInfo/default", method = RequestMethod.GET)
    @ResponseBody
    public AssetProfileInfo getDefaultAssetProfileInfo() throws EchoiotException {
        try {
            return checkNotNull(assetProfileService.findDefaultAssetProfileInfo(getTenantId()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update Asset Profile (saveAssetProfile)",
            notes = "Create or update the Asset Profile. When creating asset profile, platform generates asset profile id as " + ControllerConstants.UUID_WIKI_LINK +
                    "The newly created asset profile id will be present in the response. " +
                    "Specify existing asset profile id to update the asset profile. " +
                    "Referencing non-existing asset profile Id will cause 'Not Found' error. " + ControllerConstants.NEW_LINE +
                    "Asset profile name is unique in the scope of tenant. Only one 'default' asset profile may exist in scope of tenant. " +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Asset Profile entity. " +
                    ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json",
            consumes = "application/json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/assetProfile", method = RequestMethod.POST)
    @ResponseBody
    public AssetProfile saveAssetProfile(
            @ApiParam(value = "A JSON value representing the asset profile.")
            @RequestBody AssetProfile assetProfile) throws Exception {
        assetProfile.setTenantId(getTenantId());
        checkEntity(assetProfile.getId(), assetProfile, PerResource.ASSET_PROFILE);
        return tbAssetProfileService.save(assetProfile, getCurrentUser());
    }

    @ApiOperation(value = "Delete asset profile (deleteAssetProfile)",
            notes = "Deletes the asset profile. Referencing non-existing asset profile Id will cause an error. " +
                    "Can't delete the asset profile if it is referenced by existing assets." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/assetProfile/{assetProfileId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAssetProfile(
            @ApiParam(value = ControllerConstants.ASSET_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(ControllerConstants.ASSET_PROFILE_ID) String strAssetProfileId) throws EchoiotException {
        checkParameter(ControllerConstants.ASSET_PROFILE_ID, strAssetProfileId);
        AssetProfileId assetProfileId = new AssetProfileId(toUUID(strAssetProfileId));
        AssetProfile assetProfile = checkAssetProfileId(assetProfileId, Operation.DELETE);
        tbAssetProfileService.delete(assetProfile, getCurrentUser());
    }

    @ApiOperation(value = "Make Asset Profile Default (setDefaultAssetProfile)",
            notes = "Marks asset profile as default within a tenant scope." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/assetProfile/{assetProfileId}/default", method = RequestMethod.POST)
    @ResponseBody
    public AssetProfile setDefaultAssetProfile(
            @ApiParam(value = ControllerConstants.ASSET_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(ControllerConstants.ASSET_PROFILE_ID) String strAssetProfileId) throws EchoiotException {
        checkParameter(ControllerConstants.ASSET_PROFILE_ID, strAssetProfileId);
        AssetProfileId assetProfileId = new AssetProfileId(toUUID(strAssetProfileId));
        AssetProfile assetProfile = checkAssetProfileId(assetProfileId, Operation.WRITE);
        AssetProfile previousDefaultAssetProfile = assetProfileService.findDefaultAssetProfile(getTenantId());
        return tbAssetProfileService.setDefaultAssetProfile(assetProfile, previousDefaultAssetProfile, getCurrentUser());
    }

    @ApiOperation(value = "Get Asset Profiles (getAssetProfiles)",
            notes = "Returns a page of asset profile objects owned by tenant. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/assetProfiles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AssetProfile> getAssetProfiles(
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ControllerConstants.ASSET_PROFILE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.ASSET_PROFILE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(assetProfileService.findAssetProfiles(getTenantId(), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Asset Profile infos (getAssetProfileInfos)",
            notes = "Returns a page of asset profile info objects owned by tenant. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.ASSET_PROFILE_INFO_DESCRIPTION + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/assetProfileInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<AssetProfileInfo> getAssetProfileInfos(
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ControllerConstants.ASSET_PROFILE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.ASSET_PROFILE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(assetProfileService.findAssetProfileInfos(getTenantId(), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
