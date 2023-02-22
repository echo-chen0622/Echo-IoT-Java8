package org.echoiot.server.install;

import org.echoiot.server.dao.audit.AuditLogLevelFilter;
import org.echoiot.server.dao.audit.AuditLogLevelProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;

@Configuration
@Profile("install")
public class EchoiotInstallConfiguration {

    @Bean
    public AuditLogLevelFilter emptyAuditLogLevelFilter() {
        var props = new AuditLogLevelProperties();
        props.setMask(new HashMap<>());
        return new AuditLogLevelFilter(props);
    }
}
