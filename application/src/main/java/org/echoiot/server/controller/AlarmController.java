package org.echoiot.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.alarm.*;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.AlarmId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.TimePageLink;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.entitiy.alarm.TbAlarmService;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class AlarmController extends BaseController {

    @NotNull
    private final TbAlarmService tbAlarmService;

    public static final String ALARM_ID = "alarmId";
    private static final String ALARM_SECURITY_CHECK = "If the user has the authority of 'Tenant Administrator', the server checks that the originator of alarm is owned by the same tenant. " +
            "If the user has the authority of 'Customer User', the server checks that the originator of alarm belongs to the customer. ";
    private static final String ALARM_QUERY_SEARCH_STATUS_DESCRIPTION = "A string value representing one of the AlarmSearchStatus enumeration value";
    private static final String ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES = "ANY, ACTIVE, CLEARED, ACK, UNACK";
    private static final String ALARM_QUERY_STATUS_DESCRIPTION = "A string value representing one of the AlarmStatus enumeration value";
    private static final String ALARM_QUERY_STATUS_ALLOWABLE_VALUES = "ACTIVE_UNACK, ACTIVE_ACK, CLEARED_UNACK, CLEARED_ACK";
    private static final String ALARM_QUERY_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on of next alarm fields: type, severity or status";
    private static final String ALARM_QUERY_START_TIME_DESCRIPTION = "The start timestamp in milliseconds of the search time range over the Alarm class field: 'createdTime'.";
    private static final String ALARM_QUERY_END_TIME_DESCRIPTION = "The end timestamp in milliseconds of the search time range over the Alarm class field: 'createdTime'.";
    private static final String ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION = "A boolean value to specify if the alarm originator name will be " +
            "filled in the AlarmInfo object  field: 'originatorName' or will returns as null.";

    @ApiOperation(value = "Get Alarm (getAlarmById)",
            notes = "Fetch the Alarm object based on the provided Alarm Id. " + ALARM_SECURITY_CHECK, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}", method = RequestMethod.GET)
    @ResponseBody
    public Alarm getAlarmById(@NotNull @ApiParam(value = ControllerConstants.ALARM_ID_PARAM_DESCRIPTION)
                              @PathVariable(ALARM_ID) String strAlarmId) throws EchoiotException {
        checkParameter(ALARM_ID, strAlarmId);
        try {
            @NotNull AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
            return checkAlarmId(alarmId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Alarm Info (getAlarmInfoById)",
            notes = "Fetch the Alarm Info object based on the provided Alarm Id. " +
                    ALARM_SECURITY_CHECK + ControllerConstants.ALARM_INFO_DESCRIPTION + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/info/{alarmId}", method = RequestMethod.GET)
    @ResponseBody
    public AlarmInfo getAlarmInfoById(@NotNull @ApiParam(value = ControllerConstants.ALARM_ID_PARAM_DESCRIPTION)
                                      @PathVariable(ALARM_ID) String strAlarmId) throws EchoiotException {
        checkParameter(ALARM_ID, strAlarmId);
        try {
            @NotNull AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
            return checkAlarmInfoId(alarmId, Operation.READ);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Create or update Alarm (saveAlarm)",
            notes = "Creates or Updates the Alarm. " +
                    "When creating alarm, platform generates Alarm Id as " + ControllerConstants.UUID_WIKI_LINK +
                    "The newly created Alarm id will be present in the response. Specify existing Alarm id to update the alarm. " +
                    "Referencing non-existing Alarm Id will cause 'Not Found' error. " +
                    "\n\nPlatform also deduplicate the alarms based on the entity id of originator and alarm 'type'. " +
                    "For example, if the user or system component create the alarm with the type 'HighTemperature' for device 'Device A' the new active alarm is created. " +
                    "If the user tries to create 'HighTemperature' alarm for the same device again, the previous alarm will be updated (the 'end_ts' will be set to current timestamp). " +
                    "If the user clears the alarm (see 'Clear Alarm(clearAlarm)'), than new alarm with the same type and same device may be created. " +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Alarm entity. " +
                    ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm", method = RequestMethod.POST)
    @ResponseBody
    public Alarm saveAlarm(@NotNull @ApiParam(value = "A JSON value representing the alarm.") @RequestBody Alarm alarm) throws EchoiotException {
        alarm.setTenantId(getTenantId());
        checkEntity(alarm.getId(), alarm, Resource.ALARM);
        return tbAlarmService.save(alarm, getCurrentUser());
    }

    @ApiOperation(value = "Delete Alarm (deleteAlarm)",
            notes = "Deletes the Alarm. Referencing non-existing Alarm Id will cause an error." + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}", method = RequestMethod.DELETE)
    @ResponseBody
    public Boolean deleteAlarm(@NotNull @ApiParam(value = ControllerConstants.ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId) throws EchoiotException {
        checkParameter(ALARM_ID, strAlarmId);
        @NotNull AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.DELETE);
        return tbAlarmService.delete(alarm, getCurrentUser());
    }

    @ApiOperation(value = "Acknowledge Alarm (ackAlarm)",
            notes = "Acknowledge the Alarm. " +
                    "Once acknowledged, the 'ack_ts' field will be set to current timestamp and special rule chain event 'ALARM_ACK' will be generated. " +
                    "Referencing non-existing Alarm Id will cause an error." + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/ack", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void ackAlarm(@NotNull @ApiParam(value = ControllerConstants.ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        @NotNull AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        tbAlarmService.ack(alarm, getCurrentUser()).get();
    }

    @ApiOperation(value = "Clear Alarm (clearAlarm)",
            notes = "Clear the Alarm. " +
                    "Once cleared, the 'clear_ts' field will be set to current timestamp and special rule chain event 'ALARM_CLEAR' will be generated. " +
                    "Referencing non-existing Alarm Id will cause an error." + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{alarmId}/clear", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void clearAlarm(@NotNull @ApiParam(value = ControllerConstants.ALARM_ID_PARAM_DESCRIPTION) @PathVariable(ALARM_ID) String strAlarmId) throws Exception {
        checkParameter(ALARM_ID, strAlarmId);
        @NotNull AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
        Alarm alarm = checkAlarmId(alarmId, Operation.WRITE);
        tbAlarmService.clear(alarm, getCurrentUser()).get();
    }

    @ApiOperation(value = "Get Alarms (getAlarms)",
            notes = "Returns a page of alarms for the selected entity. Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public PageData<AlarmInfo> getAlarms(
            @ApiParam(value = ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE")
            @PathVariable(ControllerConstants.ENTITY_TYPE) String strEntityType,
            @NotNull @ApiParam(value = ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ControllerConstants.ENTITY_ID) String strEntityId,
            @ApiParam(value = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String searchStatus,
            @ApiParam(value = ALARM_QUERY_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String status,
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.ALARM_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = ALARM_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = ALARM_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @ApiParam(value = ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION)
            @RequestParam(required = false) Boolean fetchOriginator
                                        ) throws EchoiotException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        @Nullable AlarmSearchStatus alarmSearchStatus = StringUtils.isEmpty(searchStatus) ? null : AlarmSearchStatus.valueOf(searchStatus);
        @Nullable AlarmStatus alarmStatus = StringUtils.isEmpty(status) ? null : AlarmStatus.valueOf(status);
        if (alarmSearchStatus != null && alarmStatus != null) {
            throw new EchoiotException("Invalid alarms search query: Both parameters 'searchStatus' " +
                    "and 'status' can't be specified at the same time!", EchoiotErrorCode.BAD_REQUEST_PARAMS);
        }
        checkEntityId(entityId, Operation.READ);
        @NotNull TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);

        try {
            return checkNotNull(alarmService.findAlarms(getCurrentUser().getTenantId(), new AlarmQuery(entityId, pageLink, alarmSearchStatus, alarmStatus, fetchOriginator)).get());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get All Alarms (getAllAlarms)",
            notes = "Returns a page of alarms that belongs to the current user owner. " +
                    "If the user has the authority of 'Tenant Administrator', the server returns alarms that belongs to the tenant of current user. " +
                    "If the user has the authority of 'Customer User', the server returns alarms that belongs to the customer of current user. " +
                    "Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error. " +
                    ControllerConstants.PAGE_DATA_PARAMETERS + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarms", method = RequestMethod.GET)
    @ResponseBody
    public PageData<AlarmInfo> getAllAlarms(
            @ApiParam(value = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String searchStatus,
            @ApiParam(value = ALARM_QUERY_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String status,
            @ApiParam(value = ControllerConstants.PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = ControllerConstants.PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = ALARM_QUERY_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = ControllerConstants.SORT_PROPERTY_DESCRIPTION, allowableValues = ControllerConstants.ALARM_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = ControllerConstants.SORT_ORDER_DESCRIPTION, allowableValues = ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = ALARM_QUERY_START_TIME_DESCRIPTION)
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = ALARM_QUERY_END_TIME_DESCRIPTION)
            @RequestParam(required = false) Long endTime,
            @ApiParam(value = ALARM_QUERY_FETCH_ORIGINATOR_DESCRIPTION)
            @RequestParam(required = false) Boolean fetchOriginator
    ) throws EchoiotException {
        @Nullable AlarmSearchStatus alarmSearchStatus = StringUtils.isEmpty(searchStatus) ? null : AlarmSearchStatus.valueOf(searchStatus);
        @Nullable AlarmStatus alarmStatus = StringUtils.isEmpty(status) ? null : AlarmStatus.valueOf(status);
        if (alarmSearchStatus != null && alarmStatus != null) {
            throw new EchoiotException("Invalid alarms search query: Both parameters 'searchStatus' " +
                    "and 'status' can't be specified at the same time!", EchoiotErrorCode.BAD_REQUEST_PARAMS);
        }
        @NotNull TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);

        try {
            if (getCurrentUser().isCustomerUser()) {
                return checkNotNull(alarmService.findCustomerAlarms(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId(), new AlarmQuery(null, pageLink, alarmSearchStatus, alarmStatus, fetchOriginator)).get());
            } else {
                return checkNotNull(alarmService.findAlarms(getCurrentUser().getTenantId(), new AlarmQuery(null, pageLink, alarmSearchStatus, alarmStatus, fetchOriginator)).get());
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get Highest Alarm Severity (getHighestAlarmSeverity)",
            notes = "Search the alarms by originator ('entityType' and entityId') and optional 'status' or 'searchStatus' filters and returns the highest AlarmSeverity(CRITICAL, MAJOR, MINOR, WARNING or INDETERMINATE). " +
                    "Specifying both parameters 'searchStatus' and 'status' at the same time will cause an error." + ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH
            , produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarm/highestSeverity/{entityType}/{entityId}", method = RequestMethod.GET)
    @ResponseBody
    public AlarmSeverity getHighestAlarmSeverity(
            @ApiParam(value = ControllerConstants.ENTITY_TYPE_PARAM_DESCRIPTION, required = true, defaultValue = "DEVICE")
            @PathVariable(ControllerConstants.ENTITY_TYPE) String strEntityType,
            @NotNull @ApiParam(value = ControllerConstants.ENTITY_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(ControllerConstants.ENTITY_ID) String strEntityId,
            @ApiParam(value = ALARM_QUERY_SEARCH_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_SEARCH_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String searchStatus,
            @ApiParam(value = ALARM_QUERY_STATUS_DESCRIPTION, allowableValues = ALARM_QUERY_STATUS_ALLOWABLE_VALUES)
            @RequestParam(required = false) String status
    ) throws EchoiotException {
        checkParameter("EntityId", strEntityId);
        checkParameter("EntityType", strEntityType);
        EntityId entityId = EntityIdFactory.getByTypeAndId(strEntityType, strEntityId);
        @Nullable AlarmSearchStatus alarmSearchStatus = StringUtils.isEmpty(searchStatus) ? null : AlarmSearchStatus.valueOf(searchStatus);
        @Nullable AlarmStatus alarmStatus = StringUtils.isEmpty(status) ? null : AlarmStatus.valueOf(status);
        if (alarmSearchStatus != null && alarmStatus != null) {
            throw new EchoiotException("Invalid alarms search query: Both parameters 'searchStatus' " +
                    "and 'status' can't be specified at the same time!", EchoiotErrorCode.BAD_REQUEST_PARAMS);
        }
        checkEntityId(entityId, Operation.READ);
        try {
            return alarmService.findHighestAlarmSeverity(getCurrentUser().getTenantId(), entityId, alarmSearchStatus, alarmStatus);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
