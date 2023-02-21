package org.echoiot.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.coapserver.CoapServerService;
import org.echoiot.server.coapserver.TbCoapServerComponent;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.TbTransportService;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.transport.coap.efento.CoapEfentoTransportResource;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.UnknownHostException;

@Service("CoapTransportService")
@TbCoapServerComponent
@Slf4j
public class CoapTransportService implements TbTransportService {

    private static final String V1 = "v1";
    private static final String API = "api";
    private static final String EFENTO = "efento";
    public static final String MEASUREMENTS = "m";
    public static final String DEVICE_INFO = "i";
    public static final String CONFIGURATION = "c";
    public static final String CURRENT_TIMESTAMP = "t";

    @Resource
    private CoapServerService coapServerService;

    @Resource
    private CoapTransportContext coapTransportContext;

    private CoapServer coapServer;

    @PostConstruct
    public void init() throws UnknownHostException {
        log.info("Starting CoAP transport...");
        coapServer = coapServerService.getCoapServer();
        @NotNull CoapResource api = new CoapResource(API);
        api.add(new CoapTransportResource(coapTransportContext, coapServerService, V1));

        @NotNull CoapEfentoTransportResource efento = new CoapEfentoTransportResource(coapTransportContext, EFENTO);
        efento.add(new CoapResource(MEASUREMENTS));
        efento.add(new CoapResource(DEVICE_INFO));
        efento.add(new CoapResource(CONFIGURATION));
        efento.add(new CoapResource(CURRENT_TIMESTAMP));
        coapServer.add(api);
        coapServer.add(efento);
        coapServer.add(new OtaPackageTransportResource(coapTransportContext, OtaPackageType.FIRMWARE));
        coapServer.add(new OtaPackageTransportResource(coapTransportContext, OtaPackageType.SOFTWARE));
        log.info("CoAP transport started!");
    }

    @PreDestroy
    public void shutdown() {
        log.info("CoAP transport stopped!");
    }

    @NotNull
    @Override
    public String getName() {
        return DataConstants.COAP_TRANSPORT_NAME;
    }
}
