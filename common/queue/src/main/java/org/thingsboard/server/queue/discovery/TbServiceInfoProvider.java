package org.thingsboard.server.queue.discovery;

import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;

public interface TbServiceInfoProvider {

    String getServiceId();

    String getServiceType();

    ServiceInfo getServiceInfo();

    boolean isService(ServiceType serviceType);

}
