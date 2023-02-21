package org.echoiot.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
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
        @NotNull JsonNode info = buildInfoObject();
        log.info("System build info: {}", info);
    }

    @NotNull
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/system/info", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getSystemVersionInfo() {
        return buildInfoObject();
    }

    @NotNull
    private JsonNode buildInfoObject() {
        @NotNull ObjectMapper objectMapper = new ObjectMapper();
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
