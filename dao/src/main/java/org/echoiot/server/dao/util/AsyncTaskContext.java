package org.echoiot.server.dao.util;

import com.google.common.util.concurrent.SettableFuture;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Created by Echo on 24.10.18.
 */
@Data
public class AsyncTaskContext<T extends AsyncTask, V> {

    @NotNull
    private final UUID id;
    @NotNull
    private final T task;
    @NotNull
    private final SettableFuture<V> future;
    private final long createTime;

}
