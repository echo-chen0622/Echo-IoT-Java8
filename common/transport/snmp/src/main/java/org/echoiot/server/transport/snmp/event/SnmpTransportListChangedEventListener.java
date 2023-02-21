package org.echoiot.server.transport.snmp.event;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.echoiot.server.queue.discovery.TbApplicationEventListener;
import org.echoiot.server.queue.util.TbSnmpTransportComponent;
import org.echoiot.server.transport.snmp.SnmpTransportContext;

@TbSnmpTransportComponent
@Component
@RequiredArgsConstructor
public class SnmpTransportListChangedEventListener extends TbApplicationEventListener<SnmpTransportListChangedEvent> {
    @NotNull
    private final SnmpTransportContext snmpTransportContext;

    @Override
    protected void onTbApplicationEvent(SnmpTransportListChangedEvent event) {
        snmpTransportContext.onSnmpTransportListChanged();
    }
}
