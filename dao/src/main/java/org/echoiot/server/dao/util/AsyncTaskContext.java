package org.echoiot.server.dao.util;

import com.google.common.util.concurrent.SettableFuture;
import lombok.Data;

import java.util.UUID;

/**
 * Created by Echo on 24.10.18.
 */
@Data
public class AsyncTaskContext<T extends AsyncTask, V> {

    private final UUID id;
    private final T task;
    private final SettableFuture<V> future;
    private final long createTime;

}
