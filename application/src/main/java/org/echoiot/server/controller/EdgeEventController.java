package org.echoiot.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.TimePageLink;
import org.echoiot.server.dao.edge.EdgeEventService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.security.permission.Operation;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.echoiot.server.controller.ControllerConstants.*;

@Slf4j
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class EdgeEventController extends BaseController {

    @Resource
    private EdgeEventService edgeEventService;

    public static final String EDGE_ID = "edgeId";

    @ApiOperation(value = "Get Edge Events (getEdgeEvents)",
            notes = "Returns a page of edge events for the requested edge. " +
                    PAGE_DATA_PARAMETERS, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/events", method = RequestMethod.GET)
    @ResponseBody
    public PageData<EdgeEvent> getEdgeEvents(
            @NotNull @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(EDGE_ID) String strEdgeId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = "The case insensitive 'substring' filter based on the edge event type name.")
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = EDGE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @NotNull @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = "Timestamp. Edge events with creation time before it won't be queried")
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = "Timestamp. Edge events with creation time after it won't be queried")
            @RequestParam(required = false) Long endTime) throws EchoiotException {
        checkParameter(EDGE_ID, strEdgeId);
        try {
            TenantId tenantId = getCurrentUser().getTenantId();
            @NotNull EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
            checkEdgeId(edgeId, Operation.READ);
            @NotNull TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
            return checkNotNull(edgeEventService.findEdgeEvents(tenantId, edgeId, pageLink, false));
        } catch (Exception e) {
            throw handleException(e);
        }
    }
}
