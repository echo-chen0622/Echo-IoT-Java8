package org.echoiot.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.OtaPackage;
import org.echoiot.server.common.data.OtaPackageInfo;
import org.echoiot.server.common.data.SaveOtaPackageInfoRequest;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.OtaPackageId;
import org.echoiot.server.common.data.ota.ChecksumAlgorithm;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.ota.TbOtaPackageService;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
public class OtaPackageController extends BaseController {

    @NotNull
    private final TbOtaPackageService tbOtaPackageService;

    public static final String OTA_PACKAGE_ID = "otaPackageId";
    public static final String CHECKSUM_ALGORITHM = "checksumAlgorithm";

    @NotNull
    @ApiOperation(value = "Download OTA Package (downloadOtaPackage)", notes = "Download OTA Package based on the provided OTA Package Id." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority( 'TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}/download", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<org.springframework.core.io.Resource> downloadOtaPackage(@NotNull @ApiParam(value = ControllerConstants.OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                                                                   @PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws EchoiotException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        try {
            @NotNull OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            OtaPackage otaPackage = checkOtaPackageId(otaPackageId, Operation.READ);

            if (otaPackage.hasUrl()) {
                return ResponseEntity.badRequest().build();
            }

            @NotNull ByteArrayResource resource = new ByteArrayResource(otaPackage.getData().array());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + otaPackage.getFileName())
                    .header("x-filename", otaPackage.getFileName())
                    .contentLength(resource.contentLength())
                    .contentType(parseMediaType(otaPackage.getContentType()))
                    .body(resource);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get OTA Package Info (getOtaPackageInfoById)",
            notes = "Fetch the OTA Package Info object based on the provided OTA Package Id. " +
                    ControllerConstants.OTA_PACKAGE_INFO_DESCRIPTION + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackage/info/{otaPackageId}", method = RequestMethod.GET)
    @ResponseBody
    public OtaPackageInfo getOtaPackageInfoById(@NotNull @ApiParam(value = ControllerConstants.OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                                @PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws EchoiotException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        try {
            @NotNull OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            return checkNotNull(otaPackageService.findOtaPackageInfoById(getTenantId(), otaPackageId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get OTA Package (getOtaPackageById)",
            notes = "Fetch the OTA Package object based on the provided OTA Package Id. " +
                    "The server checks that the OTA Package is owned by the same tenant. " + ControllerConstants.OTA_PACKAGE_DESCRIPTION + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.GET)
    @ResponseBody
    public OtaPackage getOtaPackageById(@NotNull @ApiParam(value = ControllerConstants.OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                        @PathVariable(OTA_PACKAGE_ID) String strOtaPackageId) throws EchoiotException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        try {
            @NotNull OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
            return checkOtaPackageId(otaPackageId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create Or Update OTA Package Info (saveOtaPackageInfo)",
            notes = "Create or update the OTA Package Info. When creating OTA Package Info, platform generates OTA Package id as " + ControllerConstants.UUID_WIKI_LINK +
                    "The newly created OTA Package id will be present in the response. " +
                    "Specify existing OTA Package id to update the OTA Package Info. " +
                    "Referencing non-existing OTA Package Id will cause 'Not Found' error. " +
                    "\n\nOTA Package combination of the title with the version is unique in the scope of tenant. " + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = APPLICATION_JSON_VALUE,
            consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage", method = RequestMethod.POST)
    @ResponseBody
    public OtaPackageInfo saveOtaPackageInfo(@NotNull @ApiParam(value = "A JSON value representing the OTA Package.")
                                             @RequestBody SaveOtaPackageInfoRequest otaPackageInfo) throws EchoiotException {
        otaPackageInfo.setTenantId(getTenantId());
        checkEntity(otaPackageInfo.getId(), otaPackageInfo, Resource.OTA_PACKAGE);

        return tbOtaPackageService.save(otaPackageInfo, getCurrentUser());
    }

    @ApiOperation(value = "Save OTA Package data (saveOtaPackageData)",
            notes = "Update the OTA Package. Adds the date to the existing OTA Package Info" + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = APPLICATION_JSON_VALUE,
            consumes = MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.POST, consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public OtaPackageInfo saveOtaPackageData(@NotNull @ApiParam(value = ControllerConstants.OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                             @PathVariable(OTA_PACKAGE_ID) String strOtaPackageId,
                                             @ApiParam(value = "OTA Package checksum. For example, '0xd87f7e0c'")
                                             @RequestParam(required = false) String checksum,
                                             @NotNull @ApiParam(value = "OTA Package checksum algorithm.", allowableValues = ControllerConstants.OTA_PACKAGE_CHECKSUM_ALGORITHM_ALLOWABLE_VALUES)
                                             @RequestParam(CHECKSUM_ALGORITHM) String checksumAlgorithmStr,
                                             @NotNull @ApiParam(value = "OTA Package data.")
                                             @RequestPart MultipartFile file) throws EchoiotException, IOException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        checkParameter(CHECKSUM_ALGORITHM, checksumAlgorithmStr);
        @NotNull OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
        OtaPackageInfo otaPackageInfo = checkOtaPackageInfoId(otaPackageId, Operation.READ);
        @NotNull ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.valueOf(checksumAlgorithmStr.toUpperCase());
        @NotNull byte[] data = file.getBytes();
        return tbOtaPackageService.saveOtaPackageData(otaPackageInfo, checksum, checksumAlgorithm,
                data, file.getOriginalFilename(), file.getContentType(), getCurrentUser());
    }

    @ApiOperation(value = "Get OTA Package Infos (getOtaPackages)",
            notes = "Returns a page of OTA Package Info objects owned by tenant. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.OTA_PACKAGE_INFO_DESCRIPTION + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackages", method = RequestMethod.GET)
    @ResponseBody
    public PageData<OtaPackageInfo> getOtaPackages(@ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
                                                   @RequestParam int pageSize,
                                                   @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
                                                   @RequestParam int page,
                                                   @ApiParam(value = ControllerConstants.OTA_PACKAGE_TEXT_SEARCH_DESCRIPTION)
                                                   @RequestParam(required = false) String textSearch,
                                                   @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.OTA_PACKAGE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                   @RequestParam(required = false) String sortProperty,
                                                   @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
                                                   @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(otaPackageService.findTenantOtaPackagesByTenantId(getTenantId(), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get OTA Package Infos (getOtaPackages)",
            notes = "Returns a page of OTA Package Info objects owned by tenant. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.OTA_PACKAGE_INFO_DESCRIPTION + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/otaPackages/{deviceProfileId}/{type}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<OtaPackageInfo> getOtaPackages(@NotNull @ApiParam(value = ControllerConstants.DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
                                                   @PathVariable("deviceProfileId") String strDeviceProfileId,
                                                   @ApiParam(value = "OTA Package type.", allowableValues = "FIRMWARE, SOFTWARE")
                                                   @PathVariable("type") String strType,
                                                   @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
                                                   @RequestParam int pageSize,
                                                   @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
                                                   @RequestParam int page,
                                                   @ApiParam(value = ControllerConstants.OTA_PACKAGE_TEXT_SEARCH_DESCRIPTION)
                                                   @RequestParam(required = false) String textSearch,
                                                   @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.OTA_PACKAGE_SORT_PROPERTY_ALLOWABLE_VALUES)
                                                   @RequestParam(required = false) String sortProperty,
                                                   @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
                                                   @RequestParam(required = false) String sortOrder) throws EchoiotException {
        checkParameter("deviceProfileId", strDeviceProfileId);
        checkParameter("type", strType);
        try {
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            return checkNotNull(otaPackageService.findTenantOtaPackagesByTenantIdAndDeviceProfileIdAndTypeAndHasData(getTenantId(),
                                                                                                                     new DeviceProfileId(toUUID(strDeviceProfileId)), OtaPackageType.valueOf(strType), pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Delete OTA Package (deleteOtaPackage)",
            notes = "Deletes the OTA Package. Referencing non-existing OTA Package Id will cause an error. " +
                    "Can't delete the OTA Package if it is referenced by existing devices or device profile." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/otaPackage/{otaPackageId}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteOtaPackage(@NotNull @ApiParam(value = ControllerConstants.OTA_PACKAGE_ID_PARAM_DESCRIPTION)
                                 @PathVariable("otaPackageId") String strOtaPackageId) throws EchoiotException {
        checkParameter(OTA_PACKAGE_ID, strOtaPackageId);
        @NotNull OtaPackageId otaPackageId = new OtaPackageId(toUUID(strOtaPackageId));
        OtaPackageInfo otaPackageInfo = checkOtaPackageInfoId(otaPackageId, Operation.DELETE);
        tbOtaPackageService.delete(otaPackageInfo, getCurrentUser());
    }

}
