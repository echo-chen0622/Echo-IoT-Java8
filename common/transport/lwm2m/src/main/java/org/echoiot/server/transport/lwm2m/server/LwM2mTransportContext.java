package org.echoiot.server.transport.lwm2m.server;

import lombok.Getter;
import lombok.Setter;
import org.echoiot.server.common.transport.TransportContext;
import org.echoiot.server.queue.util.TbLwM2mTransportComponent;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.springframework.stereotype.Component;

@Component
@TbLwM2mTransportComponent
public class LwM2mTransportContext extends TransportContext {

    @Getter @Setter
    private LeshanServer server;

}
