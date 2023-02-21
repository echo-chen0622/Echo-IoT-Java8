package org.echoiot.server.controller;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.device.DeviceSearchQuery;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.page.TimePageLink;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.echoiot.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.echoiot.server.dao.device.claim.ClaimResponse;
import org.echoiot.server.dao.device.claim.ClaimResult;
import org.echoiot.server.dao.device.claim.ReclaimResult;
import org.echoiot.server.dao.exception.IncorrectParameterException;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.device.DeviceBulkImportService;
import org.echoiot.server.service.entitiy.device.TbDeviceService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.PerResource;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DeviceController extends BaseController {

    protected static final String DEVICE_NAME = "deviceName";

    @NotNull
    private final DeviceBulkImportService deviceBulkImportService;

    @NotNull
    private final TbDeviceService tbDeviceService;

    @ApiOperation(value = "Get Device (getDeviceById)",
            notes = "Fetch the Device object based on the provided Device Id. " +
                    "If the user has the authority of 'TENANT_ADMIN', the server checks that the device is owned by the same tenant. " +
                    "If the user has the authority of 'CUSTOMER_USER', the server checks that the device is assigned to the same customer." +
                    ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public Device getDeviceById(@NotNull @ApiParam(value = ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION)
                                @PathVariable(ControllerConstants.DEVICE_ID) String strDeviceId) throws EchoiotException {
        checkParameter(ControllerConstants.DEVICE_ID, strDeviceId);
        @NotNull DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        return checkDeviceId(deviceId, Operation.READ);
    }

    @ApiOperation(value = "Get Device Info (getDeviceInfoById)",
            notes = "Fetch the Device Info object based on the provided Device Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the device is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the device is assigned to the same customer. " +
                    ControllerConstants.DEVICE_INFO_DESCRIPTION + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/info/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    public DeviceInfo getDeviceInfoById(@NotNull @ApiParam(value = ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION)
                                        @PathVariable(ControllerConstants.DEVICE_ID) String strDeviceId) throws EchoiotException {
        checkParameter(ControllerConstants.DEVICE_ID, strDeviceId);
        @NotNull DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        return checkDeviceInfoId(deviceId, Operation.READ);
    }

    @ApiOperation(value = "Create Or Update Device (saveDevice)",
            notes = "Create or update the Device. When creating device, platform generates Device Id as " + ControllerConstants.UUID_WIKI_LINK +
                    "Device credentials are also generated if not provided in the 'accessToken' request parameter. " +
                    "The newly created device id will be present in the response. " +
                    "Specify existing Device id to update the device. " +
                    "Referencing non-existing device Id will cause 'Not Found' error." +
                    "\n\nDevice name is unique in the scope of tenant. Use unique identifiers like MAC or IMEI for the device names and non-unique 'label' field for user-friendly visualization purposes." +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Device entity. " +
                    ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device", method = RequestMethod.POST)
    @ResponseBody
    public Device saveDevice(@NotNull @ApiParam(value = "A JSON value representing the device.") @RequestBody Device device,
                             @ApiParam(value = "Optional value of the device credentials to be used during device creation. " +
                                     "If omitted, access token will be auto-generated.") @RequestParam(name = "accessToken", required = false) String accessToken) throws Exception {
        device.setTenantId(getCurrentUser().getTenantId());
        @org.jetbrains.annotations.Nullable Device oldDevice = null;
        if (device.getId() != null) {
            oldDevice = checkDeviceId(device.getId(), Operation.WRITE);
        } else {
            checkEntity(null, device, PerResource.DEVICE);
        }
        return tbDeviceService.save(device, oldDevice, accessToken, getCurrentUser());
    }

    @ApiOperation(value = "Create Device (saveDevice) with credentials ",
            notes = "Create or update the Device. When creating device, platform generates Device Id as " + ControllerConstants.UUID_WIKI_LINK +
                    "Requires to provide the Device Credentials object as well. Useful to create device and credentials in one request. " +
                    "You may find the example of LwM2M device and RPK credentials below: \n\n" +
                    ControllerConstants.DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_DESCRIPTION_MARKDOWN +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Device entity. " +
                    ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device-with-credentials", method = RequestMethod.POST)
    @ResponseBody
    public Device saveDeviceWithCredentials(@NotNull @ApiParam(value = "The JSON object with device and credentials. See method description above for example.")
                                            @RequestBody SaveDeviceWithCredentialsRequest deviceAndCredentials) throws EchoiotException {
        Device device = checkNotNull(deviceAndCredentials.getDevice());
        DeviceCredentials credentials = checkNotNull(deviceAndCredentials.getCredentials());
        device.setTenantId(getCurrentUser().getTenantId());
        checkEntity(device.getId(), device, PerResource.DEVICE);
        return tbDeviceService.saveDeviceWithCredentials(device, credentials, getCurrentUser());
    }

    @ApiOperation(value = "Delete device (deleteDevice)",
            notes = "Deletes the device, it's credentials and all the relations (from and to the device). Referencing non-existing device Id will cause an error." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteDevice(@NotNull @ApiParam(value = ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION)
                             @PathVariable(ControllerConstants.DEVICE_ID) String strDeviceId) throws Exception {
        checkParameter(ControllerConstants.DEVICE_ID, strDeviceId);
        @NotNull DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.DELETE);
        tbDeviceService.delete(device, getCurrentUser()).get();
    }

    @ApiOperation(value = "Assign device to customer (assignDeviceToCustomer)",
            notes = "Creates assignment of the device to customer. Customer will be able to query device afterwards." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public Device assignDeviceToCustomer(@NotNull @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION)
                                         @PathVariable("customerId") String strCustomerId,
                                         @NotNull @ApiParam(value = ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION)
                                         @PathVariable(ControllerConstants.DEVICE_ID) String strDeviceId) throws EchoiotException {
        checkParameter("customerId", strCustomerId);
        checkParameter(ControllerConstants.DEVICE_ID, strDeviceId);
        @NotNull CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        @NotNull DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        checkDeviceId(deviceId, Operation.ASSIGN_TO_CUSTOMER);
        return tbDeviceService.assignDeviceToCustomer(getTenantId(), deviceId, customer, getCurrentUser());
    }

    @ApiOperation(value = "Unassign device from customer (unassignDeviceFromCustomer)",
            notes = "Clears assignment of the device to customer. Customer will not be able to query device afterwards." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Device unassignDeviceFromCustomer(@NotNull @ApiParam(value = ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION)
                                             @PathVariable(ControllerConstants.DEVICE_ID) String strDeviceId) throws EchoiotException {
        checkParameter(ControllerConstants.DEVICE_ID, strDeviceId);
        @NotNull DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.UNASSIGN_FROM_CUSTOMER);
        if (device.getCustomerId() == null || device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            throw new IncorrectParameterException("Device isn't assigned to any customer!");
        }

        Customer customer = checkCustomerId(device.getCustomerId(), Operation.READ);

        return tbDeviceService.unassignDeviceFromCustomer(device, customer, getCurrentUser());
    }

    @ApiOperation(value = "Make device publicly available (assignDeviceToPublicCustomer)",
            notes = "Device will be available for non-authorized (not logged-in) users. " +
                    "This is useful to create dashboards that you plan to share/embed on a publicly available website. " +
                    "However, users that are logged-in and belong to different tenant will not be able to access the device." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public Device assignDeviceToPublicCustomer(@NotNull @ApiParam(value = ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION)
                                               @PathVariable(ControllerConstants.DEVICE_ID) String strDeviceId) throws EchoiotException {
        checkParameter(ControllerConstants.DEVICE_ID, strDeviceId);
        @NotNull DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        checkDeviceId(deviceId, Operation.ASSIGN_TO_CUSTOMER);
        return tbDeviceService.assignDeviceToPublicCustomer(getTenantId(), deviceId, getCurrentUser());
    }

    @ApiOperation(value = "Get Device Credentials (getDeviceCredentialsByDeviceId)",
            notes = "If during device creation there wasn't specified any credentials, platform generates random 'ACCESS_TOKEN' credentials." + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}/credentials", method = RequestMethod.GET)
    @ResponseBody
    public DeviceCredentials getDeviceCredentialsByDeviceId(@NotNull @ApiParam(value = ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION)
                                                            @PathVariable(ControllerConstants.DEVICE_ID) String strDeviceId) throws EchoiotException {
        checkParameter(ControllerConstants.DEVICE_ID, strDeviceId);
        @NotNull DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.READ_CREDENTIALS);
        return tbDeviceService.getDeviceCredentialsByDeviceId(device, getCurrentUser());
    }

    @ApiOperation(value = "Update device credentials (updateDeviceCredentials)", notes = "During device creation, platform generates random 'ACCESS_TOKEN' credentials. " +
                                                                                         "Use this method to update the device credentials. First use 'getDeviceCredentialsByDeviceId' to get the credentials id and value. " +
                                                                                         "Then use current method to update the credentials type and value. It is not possible to create multiple device credentials for the same device. " +
                                                                                         "The structure of device credentials id and value is simple for the 'ACCESS_TOKEN' but is much more complex for the 'MQTT_BASIC' or 'LWM2M_CREDENTIALS'." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/device/credentials", method = RequestMethod.POST)
    @ResponseBody
    public DeviceCredentials updateDeviceCredentials(
            @NotNull @ApiParam(value = "A JSON value representing the device credentials.")
            @RequestBody DeviceCredentials deviceCredentials) throws EchoiotException {
        checkNotNull(deviceCredentials);
        Device device = checkDeviceId(deviceCredentials.getDeviceId(), Operation.WRITE_CREDENTIALS);
        return tbDeviceService.updateDeviceCredentials(device, deviceCredentials, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Devices (getTenantDevices)",
            notes = "Returns a page of devices owned by tenant. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/devices", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Device> getTenantDevices(
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @org.jetbrains.annotations.Nullable @ApiParam(value = ControllerConstants.DEVICE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = ControllerConstants.DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.DEVICE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws EchoiotException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(deviceService.findDevicesByTenantIdAndType(tenantId, type, pageLink));
            } else {
                return checkNotNull(deviceService.findDevicesByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Device Infos (getTenantDeviceInfos)",
            notes = "Returns a page of devices info objects owned by tenant. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.DEVICE_INFO_DESCRIPTION + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/deviceInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DeviceInfo> getTenantDeviceInfos(
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @org.jetbrains.annotations.Nullable @ApiParam(value = ControllerConstants.DEVICE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @org.jetbrains.annotations.Nullable @ApiParam(value = ControllerConstants.DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(required = false) String deviceProfileId,
            @ApiParam(value = ControllerConstants.DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.DEVICE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder
    ) throws EchoiotException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            @NotNull PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
            if (type != null && type.trim().length() > 0) {
                return checkNotNull(deviceService.findDeviceInfosByTenantIdAndType(tenantId, type, pageLink));
            } else if (deviceProfileId != null && deviceProfileId.length() > 0) {
                @NotNull DeviceProfileId profileId = new DeviceProfileId(toUUID(deviceProfileId));
                return checkNotNull(deviceService.findDeviceInfosByTenantIdAndDeviceProfileId(tenantId, profileId, pageLink));
            } else {
                return checkNotNull(deviceService.findDeviceInfosByTenantId(tenantId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Tenant Device (getTenantDevice)",
            notes = "Requested device must be owned by tenant that the user belongs to. " +
                    "Device name is an unique property of device. So it can be used to identify the device." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/devices", params = {"deviceName"}, method = RequestMethod.GET)
    @ResponseBody
    public Device getTenantDevice(
            @ApiParam(value = ControllerConstants.DEVICE_NAME_DESCRIPTION)
            @RequestParam String deviceName) throws EchoiotException {
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            return checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Devices (getCustomerDevices)",
            notes = "Returns a page of devices objects assigned to customer. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/devices", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Device> getCustomerDevices(
            @NotNull @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ControllerConstants.CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @org.jetbrains.annotations.Nullable @ApiParam(value = ControllerConstants.DEVICE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = ControllerConstants.DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.DEVICE_SORT_PROPERTY_ALLOWABLE_VALUES)
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
                return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else {
                return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Customer Device Infos (getCustomerDeviceInfos)",
            notes = "Returns a page of devices info objects assigned to customer. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.DEVICE_INFO_DESCRIPTION + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/deviceInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<DeviceInfo> getCustomerDeviceInfos(
            @NotNull @ApiParam(value = ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("customerId") String strCustomerId,
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @org.jetbrains.annotations.Nullable @ApiParam(value = ControllerConstants.DEVICE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @org.jetbrains.annotations.Nullable @ApiParam(value = ControllerConstants.DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(required = false) String deviceProfileId,
            @ApiParam(value = ControllerConstants.DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.DEVICE_SORT_PROPERTY_ALLOWABLE_VALUES)
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
                return checkNotNull(deviceService.findDeviceInfosByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
            } else if (deviceProfileId != null && deviceProfileId.length() > 0) {
                @NotNull DeviceProfileId profileId = new DeviceProfileId(toUUID(deviceProfileId));
                return checkNotNull(deviceService.findDeviceInfosByTenantIdAndCustomerIdAndDeviceProfileId(tenantId, customerId, profileId, pageLink));
            } else {
                return checkNotNull(deviceService.findDeviceInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink));
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Devices By Ids (getDevicesByIds)",
            notes = "Requested devices must be owned by tenant or assigned to customer which user is performing the request. " + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices", params = {"deviceIds"}, method = RequestMethod.GET)
    @ResponseBody
    public List<Device> getDevicesByIds(
            @NotNull @ApiParam(value = "A list of devices ids, separated by comma ','")
            @RequestParam("deviceIds") String[] strDeviceIds) throws EchoiotException {
        checkArrayParameter("deviceIds", strDeviceIds);
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            CustomerId customerId = user.getCustomerId();
            @NotNull List<DeviceId> deviceIds = new ArrayList<>();
            for (@NotNull String strDeviceId : strDeviceIds) {
                deviceIds.add(new DeviceId(toUUID(strDeviceId)));
            }
            ListenableFuture<List<Device>> devices;
            if (customerId == null || customerId.isNullUid()) {
                devices = deviceService.findDevicesByTenantIdAndIdsAsync(tenantId, deviceIds);
            } else {
                devices = deviceService.findDevicesByTenantIdCustomerIdAndIdsAsync(tenantId, customerId, deviceIds);
            }
            return checkNotNull(devices.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @NotNull
    @ApiOperation(value = "Find related devices (findByQuery)",
            notes = "Returns all devices that are related to the specific entity. " +
                    "The entity id, relation type, device types, depth of the search, and other query parameters defined using complex 'DeviceSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info." + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices", method = RequestMethod.POST)
    @ResponseBody
    public List<Device> findByQuery(
            @NotNull @ApiParam(value = "The device search query JSON")
            @RequestBody DeviceSearchQuery query) throws EchoiotException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getDeviceTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        try {
            List<Device> devices = checkNotNull(deviceService.findDevicesByQuery(getCurrentUser().getTenantId(), query).get());
            devices = devices.stream().filter(device -> {
                try {
                    accessControlService.checkPermission(getCurrentUser(), PerResource.DEVICE, Operation.READ, device.getId(), device);
                    return true;
                } catch (EchoiotException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            return devices;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Device Types (getDeviceTypes)",
            notes = "Returns a set of unique device profile names based on devices that are either owned by the tenant or assigned to the customer which user is performing the request."
                    + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/types", method = RequestMethod.GET)
    @ResponseBody
    public List<EntitySubtype> getDeviceTypes() throws EchoiotException {
        try {
            SecurityUser user = getCurrentUser();
            TenantId tenantId = user.getTenantId();
            ListenableFuture<List<EntitySubtype>> deviceTypes = deviceService.findDeviceTypesByTenantId(tenantId);
            return checkNotNull(deviceTypes.get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @NotNull
    @ApiOperation(value = "Claim device (claimDevice)",
            notes = "Claiming makes it possible to assign a device to the specific customer using device/server side claiming data (in the form of secret key)." +
                    "To make this happen you have to provide unique device name and optional claiming data (it is needed only for device-side claiming)." +
                    "Once device is claimed, the customer becomes its owner and customer users may access device data as well as control the device. \n" +
                    "In order to enable claiming devices feature a system parameter security.claim.allowClaimingByDefault should be set to true, " +
                    "otherwise a server-side claimingAllowed attribute with the value true is obligatory for provisioned devices. \n" +
                    "See official documentation for more details regarding claiming." + ControllerConstants.CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('CUSTOMER_USER')")
    @RequestMapping(value = "/customer/device/{deviceName}/claim", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> claimDevice(@ApiParam(value = "Unique name of the device which is going to be claimed")
                                                      @PathVariable(DEVICE_NAME) String deviceName,
                                                      @NotNull @ApiParam(value = "Claiming request which can optionally contain secret key")
                                                      @RequestBody(required = false) ClaimRequest claimRequest) throws EchoiotException {
        checkParameter(DEVICE_NAME, deviceName);
        @NotNull final DeferredResult<ResponseEntity> deferredResult = new DeferredResult<>();

        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        CustomerId customerId = user.getCustomerId();

        Device device = checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
        accessControlService.checkPermission(user, PerResource.DEVICE, Operation.CLAIM_DEVICES,
                                             device.getId(), device);
        @NotNull String secretKey = getSecretKey(claimRequest);

        ListenableFuture<ClaimResult> future = tbDeviceService.claimDevice(tenantId, device, customerId, secretKey, user);

        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable ClaimResult result) {
                HttpStatus status;
                if (result != null) {
                    if (result.getResponse().equals(ClaimResponse.SUCCESS)) {
                        status = HttpStatus.OK;
                        deferredResult.setResult(new ResponseEntity<>(result, status));
                    } else {
                        status = HttpStatus.BAD_REQUEST;
                        deferredResult.setResult(new ResponseEntity<>(result.getResponse(), status));
                    }
                } else {
                    deferredResult.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                deferredResult.setErrorResult(t);
            }
        }, MoreExecutors.directExecutor());
        return deferredResult;
    }

    @NotNull
    @ApiOperation(value = "Reclaim device (reClaimDevice)",
            notes = "Reclaiming means the device will be unassigned from the customer and the device will be available for claiming again."
                    + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/device/{deviceName}/claim", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<ResponseEntity> reClaimDevice(@ApiParam(value = "Unique name of the device which is going to be reclaimed")
                                                        @PathVariable(DEVICE_NAME) String deviceName) throws EchoiotException {
        checkParameter(DEVICE_NAME, deviceName);
        @NotNull final DeferredResult<ResponseEntity> deferredResult = new DeferredResult<>();

        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();

        Device device = checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
        accessControlService.checkPermission(user, PerResource.DEVICE, Operation.CLAIM_DEVICES,
                                             device.getId(), device);

        ListenableFuture<ReclaimResult> result = tbDeviceService.reclaimDevice(tenantId, device, user);
        Futures.addCallback(result, new FutureCallback<>() {
            @Override
            public void onSuccess(ReclaimResult reclaimResult) {
                deferredResult.setResult(new ResponseEntity(HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable t) {
                deferredResult.setErrorResult(t);
            }
        }, MoreExecutors.directExecutor());
        return deferredResult;
    }

    @NotNull
    private String getSecretKey(@NotNull ClaimRequest claimRequest) {
        String secretKey = claimRequest.getSecretKey();
        if (secretKey != null) {
            return secretKey;
        }
        return DataConstants.DEFAULT_SECRET_KEY;
    }

    @ApiOperation(value = "Assign device to tenant (assignDeviceToTenant)",
            notes = "Creates assignment of the device to tenant. Thereafter tenant will be able to reassign the device to a customer." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public Device assignDeviceToTenant(@NotNull @ApiParam(value = ControllerConstants.TENANT_ID_PARAM_DESCRIPTION)
                                       @PathVariable(ControllerConstants.TENANT_ID) String strTenantId,
                                       @NotNull @ApiParam(value = ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION)
                                       @PathVariable(ControllerConstants.DEVICE_ID) String strDeviceId) throws EchoiotException {
        checkParameter(ControllerConstants.TENANT_ID, strTenantId);
        checkParameter(ControllerConstants.DEVICE_ID, strDeviceId);
        @NotNull DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.ASSIGN_TO_TENANT);

        @NotNull TenantId newTenantId = TenantId.fromUUID(toUUID(strTenantId));
        Tenant newTenant = tenantService.findTenantById(newTenantId);
        if (newTenant == null) {
            throw new EchoiotException("Could not find the specified Tenant!", EchoiotErrorCode.BAD_REQUEST_PARAMS);
        }
        return tbDeviceService.assignDeviceToTenant(device, newTenant, getCurrentUser());
    }

    @ApiOperation(value = "Assign device to edge (assignDeviceToEdge)",
            notes = "Creates assignment of an existing device to an instance of The Edge. " +
                    ControllerConstants.EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive a copy of assignment device " +
                    ControllerConstants.EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once device will be delivered to edge service, it's going to be available for usage on remote edge instance." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    public Device assignDeviceToEdge(@NotNull @ApiParam(value = ControllerConstants.EDGE_ID_PARAM_DESCRIPTION)
                                     @PathVariable(EdgeController.EDGE_ID) String strEdgeId,
                                     @NotNull @ApiParam(value = ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION)
                                     @PathVariable(ControllerConstants.DEVICE_ID) String strDeviceId) throws EchoiotException {
        checkParameter(EdgeController.EDGE_ID, strEdgeId);
        checkParameter(ControllerConstants.DEVICE_ID, strDeviceId);
        @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        @NotNull DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        checkDeviceId(deviceId, Operation.READ);

        return tbDeviceService.assignDeviceToEdge(getTenantId(), deviceId, edge, getCurrentUser());
    }

    @ApiOperation(value = "Unassign device from edge (unassignDeviceFromEdge)",
            notes = "Clears assignment of the device to the edge. " +
                    ControllerConstants.EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive an 'unassign' command to remove device " +
                    ControllerConstants.EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove device locally." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Device unassignDeviceFromEdge(@NotNull @ApiParam(value = ControllerConstants.EDGE_ID_PARAM_DESCRIPTION)
                                         @PathVariable(EdgeController.EDGE_ID) String strEdgeId,
                                         @NotNull @ApiParam(value = ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION)
                                         @PathVariable(ControllerConstants.DEVICE_ID) String strDeviceId) throws EchoiotException {
        checkParameter(EdgeController.EDGE_ID, strEdgeId);
        checkParameter(ControllerConstants.DEVICE_ID, strDeviceId);
        @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        @NotNull DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.READ);
        return tbDeviceService.unassignDeviceFromEdge(device, edge, getCurrentUser());
    }

    @ApiOperation(value = "Get devices assigned to edge (getEdgeDevices)",
            notes = "Returns a page of devices assigned to edge. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}/devices", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    public PageData<Device> getEdgeDevices(
            @NotNull @ApiParam(value = ControllerConstants.EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(EdgeController.EDGE_ID) String strEdgeId,
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @org.jetbrains.annotations.Nullable @ApiParam(value = ControllerConstants.DEVICE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = ControllerConstants.DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.DEVICE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = "Timestamp. Devices with creation time before it won't be queried")
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = "Timestamp. Devices with creation time after it won't be queried")
            @RequestParam(required = false) Long endTime) throws EchoiotException {
        checkParameter(EdgeController.EDGE_ID, strEdgeId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            checkEdgeId(edgeId, Operation.READ);
            @NotNull TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            PageData<Device> nonFilteredResult;
            if (type != null && type.trim().length() > 0) {
                nonFilteredResult = deviceService.findDevicesByTenantIdAndEdgeIdAndType(tenantId, edgeId, type, pageLink);
            } else {
                nonFilteredResult = deviceService.findDevicesByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            }
            @NotNull List<Device> filteredDevices = nonFilteredResult.getData().stream().filter(device -> {
                try {
                    accessControlService.checkPermission(getCurrentUser(), PerResource.DEVICE, Operation.READ, device.getId(), device);
                    return true;
                } catch (EchoiotException e) {
                    return false;
                }
            }).collect(Collectors.toList());
            @NotNull PageData<Device> filteredResult = new PageData<>(filteredDevices,
                                                                      nonFilteredResult.getTotalPages(),
                                                                      nonFilteredResult.getTotalElements(),
                                                                      nonFilteredResult.hasNext());
            return checkNotNull(filteredResult);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Count devices by device profile  (countByDeviceProfileAndEmptyOtaPackage)",
            notes = "The platform gives an ability to load OTA (over-the-air) packages to devices. " +
                    "It can be done in two different ways: device scope or device profile scope." +
                    "In the response you will find the number of devices with specified device profile, but without previously defined device scope OTA package. " +
                    "It can be useful when you want to define number of devices that will be affected with future OTA package" + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices/count/{otaPackageType}/{deviceProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public Long countByDeviceProfileAndEmptyOtaPackage
            (@ApiParam(value = "OTA package type", allowableValues = "FIRMWARE, SOFTWARE")
             @PathVariable("otaPackageType") String otaPackageType,
             @NotNull @ApiParam(value = "Device Profile Id. I.g. '784f394c-42b6-435a-983c-b7beff2784f9'")
             @PathVariable("deviceProfileId") String deviceProfileId) throws EchoiotException {
        checkParameter("OtaPackageType", otaPackageType);
        checkParameter("DeviceProfileId", deviceProfileId);
        try {
            return deviceService.countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage(
                    getTenantId(),
                    new DeviceProfileId(UUID.fromString(deviceProfileId)),
                    OtaPackageType.valueOf(otaPackageType));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @NotNull
    @ApiOperation(value = "Import the bulk of devices (processDevicesBulkImport)",
            notes = "There's an ability to import the bulk of devices using the only .csv file." + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping("/device/bulk_import")
    public BulkImportResult<Device> processDevicesBulkImport(@NotNull @RequestBody BulkImportRequest request) throws
            Exception {
        SecurityUser user = getCurrentUser();
        return deviceBulkImportService.processBulkImport(request, user);
    }

}
