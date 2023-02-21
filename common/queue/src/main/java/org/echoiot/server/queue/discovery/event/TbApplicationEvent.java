package org.echoiot.server.queue.discovery.event;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEvent;

import java.util.concurrent.atomic.AtomicInteger;

@ToString
public class TbApplicationEvent extends ApplicationEvent {

    private static final long serialVersionUID = 3884264064887765146L;

    private static final AtomicInteger sequence = new AtomicInteger();

    @Getter
    private final int sequenceNumber;

    public TbApplicationEvent(@NotNull Object source) {
        super(source);
        sequenceNumber = sequence.incrementAndGet();
    }

}
