package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.queue.util.TbCoreComponent;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.PostConstruct;

@ApiIgnore
@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class SystemInfoController {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @PostConstruct
    public void init() {
        JsonNode info = buildInfoObject();
        log.info("System build info: {}", info);
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/system/info", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getSystemVersionInfo() {
        return buildInfoObject();
    }

    private JsonNode buildInfoObject() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode infoObject = objectMapper.createObjectNode();
        if (buildProperties != null) {
            infoObject.put("version", buildProperties.getVersion());
            infoObject.put("artifact", buildProperties.getArtifact());
            infoObject.put("name", buildProperties.getName());
        } else {
            infoObject.put("version", "unknown");
        }
        return infoObject;
    }
}
