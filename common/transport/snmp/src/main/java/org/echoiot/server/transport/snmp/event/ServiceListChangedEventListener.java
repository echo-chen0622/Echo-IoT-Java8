package org.echoiot.server.transport.snmp.event;

import lombok.RequiredArgsConstructor;
import org.echoiot.server.transport.snmp.service.SnmpTransportBalancingService;
import org.springframework.stereotype.Component;
import org.echoiot.server.queue.discovery.TbApplicationEventListener;
import org.echoiot.server.queue.discovery.event.ServiceListChangedEvent;
import org.echoiot.server.queue.util.TbSnmpTransportComponent;

@TbSnmpTransportComponent
@Component
@RequiredArgsConstructor
public class ServiceListChangedEventListener extends TbApplicationEventListener<ServiceListChangedEvent> {
    private final SnmpTransportBalancingService snmpTransportBalancingService;

    @Override
    protected void onTbApplicationEvent(ServiceListChangedEvent event) {
        snmpTransportBalancingService.onServiceListChanged(event);
    }
}
