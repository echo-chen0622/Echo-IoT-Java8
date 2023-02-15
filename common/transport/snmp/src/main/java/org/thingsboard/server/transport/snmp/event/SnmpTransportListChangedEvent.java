package org.thingsboard.server.transport.snmp.event;

import org.thingsboard.server.queue.discovery.event.TbApplicationEvent;

public class SnmpTransportListChangedEvent extends TbApplicationEvent {
    public SnmpTransportListChangedEvent() {
        super(new Object());
    }
}
