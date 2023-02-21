package org.echoiot.server.queue.discovery.event;

import lombok.Getter;
import lombok.ToString;
import org.echoiot.server.gen.transport.TransportProtos.ServiceInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
@ToString
public class ServiceListChangedEvent extends TbApplicationEvent {
    private final List<ServiceInfo> otherServices;
    private final ServiceInfo currentService;

    public ServiceListChangedEvent(@NotNull List<ServiceInfo> otherServices, ServiceInfo currentService) {
        super(otherServices);
        this.otherServices = otherServices;
        this.currentService = currentService;
    }
}
