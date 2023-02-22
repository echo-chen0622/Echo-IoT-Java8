package org.echoiot.server.transport.lwm2m.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.TbProperty;
import org.echoiot.server.common.transport.config.ssl.SslCredentials;
import org.echoiot.server.common.transport.config.ssl.SslCredentialsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core')  && '${transport.lwm2m.enabled:false}'=='true'")
@ConfigurationProperties(prefix = "transport.lwm2m")
public class LwM2MTransportServerConfig implements LwM2MSecureServerConfig {

    @Getter
    @Value("${transport.lwm2m.dtls.retransmission_timeout:9000}")
    private int dtlsRetransmissionTimeout;

    @Getter
    @Value("${transport.lwm2m.timeout:}")
    private Long timeout;

    @Getter
    @Value("${transport.sessions.report_timeout}")
    private long sessionReportTimeout;

    @Getter
    @Value("${transport.lwm2m.security.recommended_ciphers:}")
    private boolean recommendedCiphers;

    @Getter
    @Value("${transport.lwm2m.security.recommended_supported_groups:}")
    private boolean recommendedSupportedGroups;

    @Getter
    @Value("${transport.lwm2m.downlink_pool_size:}")
    private int downlinkPoolSize;

    @Getter
    @Value("${transport.lwm2m.uplink_pool_size:}")
    private int uplinkPoolSize;

    @Getter
    @Value("${transport.lwm2m.ota_pool_size:}")
    private int otaPoolSize;

    @Getter
    @Value("${transport.lwm2m.clean_period_in_sec:}")
    private int cleanPeriodInSec;

    @Getter
    @Value("${transport.lwm2m.server.id:}")
    private Integer id;

    @Getter
    @Value("${transport.lwm2m.server.bind_address:}")
    private String host;

    @Getter
    @Value("${transport.lwm2m.server.bind_port:}")
    private Integer port;

    @Getter
    @Value("${transport.lwm2m.server.security.bind_address:}")
    private String secureHost;

    @Getter
    @Value("${transport.lwm2m.server.security.bind_port:}")
    private Integer securePort;

    @Getter
    @Value("${transport.lwm2m.psm_activity_timer:10000}")
    private long psmActivityTimer;

    @Getter
    @Value("${transport.lwm2m.paging_transmission_window:10000}")
    private long pagingTransmissionWindow;

    @Getter
    @Setter
    private List<TbProperty> networkConfig;

    @Bean
    @ConfigurationProperties(prefix = "transport.lwm2m.server.security.credentials")
    public SslCredentialsConfig lwm2mServerCredentials() {
        return new SslCredentialsConfig("LWM2M Server DTLS Credentials", false);
    }

    @Resource(name = "lwm2mServerCredentials")
    private SslCredentialsConfig credentialsConfig;

    @Bean
    @ConfigurationProperties(prefix = "transport.lwm2m.security.trust-credentials")
    public SslCredentialsConfig lwm2mTrustCredentials() {
        return new SslCredentialsConfig("LWM2M Trust Credentials", true);
    }

    @Resource(name = "lwm2mTrustCredentials")
    private SslCredentialsConfig trustCredentialsConfig;

    @Override
    public SslCredentials getSslCredentials() {
        return this.credentialsConfig.getCredentials();
    }

    public SslCredentials getTrustSslCredentials() {
        return this.trustCredentialsConfig.getCredentials();
    }
}
