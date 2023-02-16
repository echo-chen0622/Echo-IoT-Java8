package org.echoiot.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.*;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.query.EntityQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import static org.echoiot.server.controller.ControllerConstants.*;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class EntityQueryController extends BaseController {

    @Autowired
    private EntityQueryService entityQueryService;

    private static final int MAX_PAGE_SIZE = 100;

    @ApiOperation(value = "Count Entities by Query", notes = ENTITY_COUNT_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/count", method = RequestMethod.POST)
    @ResponseBody
    public long countEntitiesByQuery(
            @ApiParam(value = "A JSON value representing the entity count query. See API call notes above for more details.")
            @RequestBody EntityCountQuery query) throws EchoiotException {
        checkNotNull(query);
        try {
            return this.entityQueryService.countEntitiesByQuery(getCurrentUser(), query);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find Entity Data by Query", notes = ENTITY_DATA_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/find", method = RequestMethod.POST)
    @ResponseBody
    public PageData<EntityData> findEntityDataByQuery(
            @ApiParam(value = "A JSON value representing the entity data query. See API call notes above for more details.")
            @RequestBody EntityDataQuery query) throws EchoiotException {
        checkNotNull(query);
        try {
            return this.entityQueryService.findEntityDataByQuery(getCurrentUser(), query);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find Alarms by Query", notes = ALARM_DATA_QUERY_DESCRIPTION)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/alarmsQuery/find", method = RequestMethod.POST)
    @ResponseBody
    public PageData<AlarmData> findAlarmDataByQuery(
            @ApiParam(value = "A JSON value representing the alarm data query. See API call notes above for more details.")
            @RequestBody AlarmDataQuery query) throws EchoiotException {
        checkNotNull(query);
        try {
            return this.entityQueryService.findAlarmDataByQuery(getCurrentUser(), query);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Find Entity Keys by Query",
            notes = "Uses entity data query (see 'Find Entity Data by Query') to find first 100 entities. Then fetch and return all unique time-series and/or attribute keys. Used mostly for UI hints.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/entitiesQuery/find/keys", method = RequestMethod.POST)
    @ResponseBody
    public DeferredResult<ResponseEntity> findEntityTimeseriesAndAttributesKeysByQuery(
            @ApiParam(value = "A JSON value representing the entity data query. See API call notes above for more details.")
            @RequestBody EntityDataQuery query,
            @ApiParam(value = "Include all unique time-series keys to the result.")
            @RequestParam("timeseries") boolean isTimeseries,
            @ApiParam(value = "Include all unique attribute keys to the result.")
            @RequestParam("attributes") boolean isAttributes) throws EchoiotException {
        TenantId tenantId = getTenantId();
        checkNotNull(query);
        try {
            EntityDataPageLink pageLink = query.getPageLink();
            if (pageLink.getPageSize() > MAX_PAGE_SIZE) {
                pageLink.setPageSize(MAX_PAGE_SIZE);
            }
            return entityQueryService.getKeysByQuery(getCurrentUser(), tenantId, query, isTimeseries, isAttributes);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
