package org.echoiot.server.common.data.sync.ie.importing.csv;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class BulkImportResult<E> {
    @NotNull
    private AtomicInteger created = new AtomicInteger();
    @NotNull
    private AtomicInteger updated = new AtomicInteger();
    @NotNull
    private AtomicInteger errors = new AtomicInteger();
    @NotNull
    private Collection<String> errorsList = new ConcurrentLinkedDeque<>();
}
