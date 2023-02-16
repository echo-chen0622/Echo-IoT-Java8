package org.echoiot.server.controller;

import io.swagger.annotations.ApiOperation;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class UiSettingsController extends BaseController {

    @Value("${ui.help.base-url}")
    private String helpBaseUrl;

    @ApiOperation(value = "Get UI help base url (getHelpBaseUrl)",
            notes = "Get UI help base url used to fetch help assets. " +
                    "The actual value of the base url is configurable in the system configuration file.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/uiSettings/helpBaseUrl", method = RequestMethod.GET)
    @ResponseBody
    public String getHelpBaseUrl() throws EchoiotException {
        return helpBaseUrl;
    }

}
