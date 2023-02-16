package org.echoiot.server.queue.discovery;

import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.gen.transport.TransportProtos.ServiceInfo;

public interface TbServiceInfoProvider {

    String getServiceId();

    String getServiceType();

    ServiceInfo getServiceInfo();

    boolean isService(ServiceType serviceType);

}
