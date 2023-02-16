package org.echoiot.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.SaveDeviceWithCredentialsRequest;
import org.echoiot.server.common.data.device.profile.lwm2m.bootstrap.LwM2MServerSecurityConfigDefault;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.security.DeviceCredentials;
import org.echoiot.server.service.lwm2m.LwM2MService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.echoiot.server.controller.ControllerConstants.IS_BOOTSTRAP_SERVER_PARAM_DESCRIPTION;
import static org.echoiot.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

@Slf4j
@RestController
@ConditionalOnExpression("('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core') && '${transport.lwm2m.enabled:false}'=='true'")
@RequestMapping("/api")
public class Lwm2mController extends BaseController {

    @Autowired
    private DeviceController deviceController;

    @Autowired
    private LwM2MService lwM2MService;

    public static final String IS_BOOTSTRAP_SERVER = "isBootstrapServer";

    @ApiOperation(value = "Get Lwm2m Bootstrap SecurityInfo (getLwm2mBootstrapSecurityInfo)",
            notes = "Get the Lwm2m Bootstrap SecurityInfo object (of the current server) based on the provided isBootstrapServer parameter. If isBootstrapServer == true, get the parameters of the current Bootstrap Server. If isBootstrapServer == false, get the parameters of the current Lwm2m Server. Used for client settings when starting the client in Bootstrap mode. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/lwm2m/deviceProfile/bootstrap/{isBootstrapServer}", method = RequestMethod.GET)
    @ResponseBody
    public LwM2MServerSecurityConfigDefault getLwm2mBootstrapSecurityInfo(
        @ApiParam(value = IS_BOOTSTRAP_SERVER_PARAM_DESCRIPTION)
        @PathVariable(IS_BOOTSTRAP_SERVER) boolean bootstrapServer) throws EchoiotException {
            try {
                return lwM2MService.getServerSecurityInfo(bootstrapServer);
            } catch (Exception e) {
                throw handleException(e);
            }
    }

    @ApiOperation(hidden = true, value = "Save device with credentials (Deprecated)")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/lwm2m/device-credentials", method = RequestMethod.POST)
    @ResponseBody
    public Device saveDeviceWithCredentials(@RequestBody Map<Class<?>, Object> deviceWithDeviceCredentials) throws EchoiotException {
        ObjectMapper mapper = new ObjectMapper();
        Device device = checkNotNull(mapper.convertValue(deviceWithDeviceCredentials.get(Device.class), Device.class));
        DeviceCredentials credentials = checkNotNull(mapper.convertValue(deviceWithDeviceCredentials.get(DeviceCredentials.class), DeviceCredentials.class));
        return deviceController.saveDeviceWithCredentials(new SaveDeviceWithCredentialsRequest(device, credentials));
    }
}
