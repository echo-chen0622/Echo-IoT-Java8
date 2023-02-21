package org.echoiot.server.transport.snmp.event;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.queue.discovery.TbApplicationEventListener;
import org.echoiot.server.queue.discovery.event.ServiceListChangedEvent;
import org.echoiot.server.queue.util.TbSnmpTransportComponent;
import org.echoiot.server.transport.snmp.service.SnmpTransportBalancingService;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@TbSnmpTransportComponent
@Component
@RequiredArgsConstructor
public class ServiceListChangedEventListener extends TbApplicationEventListener<ServiceListChangedEvent> {
    @NotNull
    private final SnmpTransportBalancingService snmpTransportBalancingService;

    @Override
    protected void onTbApplicationEvent(ServiceListChangedEvent event) {
        snmpTransportBalancingService.onServiceListChanged(event);
    }
}
