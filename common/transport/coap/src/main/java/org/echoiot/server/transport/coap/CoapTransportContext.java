package org.echoiot.server.transport.coap;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.transport.TransportContext;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.coap.adaptors.JsonCoapAdaptor;
import org.echoiot.server.transport.coap.adaptors.ProtoCoapAdaptor;
import org.echoiot.server.transport.coap.client.CoapClientContext;
import org.echoiot.server.transport.coap.efento.adaptor.EfentoCoapAdaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * Created by Echo on 18.10.18.
 */
@Slf4j
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.coap.enabled}'=='true')")
@Component
@Getter
public class CoapTransportContext extends TransportContext {

    @Value("${transport.sessions.report_timeout}")
    private long sessionReportTimeout;

    @Resource
    private JsonCoapAdaptor jsonCoapAdaptor;

    @Resource
    private ProtoCoapAdaptor protoCoapAdaptor;

    @Resource
    private EfentoCoapAdaptor efentoCoapAdaptor;

    @Resource
    private CoapClientContext clientContext;

    private final ConcurrentMap<Integer, TransportProtos.ToDeviceRpcRequestMsg> rpcAwaitingAck = new ConcurrentHashMap<>();

}
