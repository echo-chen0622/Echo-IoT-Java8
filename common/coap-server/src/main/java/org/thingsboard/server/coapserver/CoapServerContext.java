package org.thingsboard.server.coapserver;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@TbCoapServerComponent
@Component
public class CoapServerContext {

    @Getter
    @Value("${transport.coap.bind_address}")
    private String host;

    @Getter
    @Value("${transport.coap.bind_port}")
    private Integer port;

    @Getter
    @Value("${transport.coap.timeout}")
    private Long timeout;

    @Getter
    @Value("${transport.coap.piggyback_timeout}")
    private Long piggybackTimeout;

    @Getter
    @Value("${transport.coap.psm_activity_timer:10000}")
    private long psmActivityTimer;

    @Getter
    @Value("${transport.coap.paging_transmission_window:10000}")
    private long pagingTransmissionWindow;

    @Getter
    @Autowired(required = false)
    private TbCoapDtlsSettings dtlsSettings;

}
