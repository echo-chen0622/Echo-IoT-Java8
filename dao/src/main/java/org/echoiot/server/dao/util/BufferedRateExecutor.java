package org.echoiot.server.dao.util;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Created by Echo on 24.10.18.
 */
public interface BufferedRateExecutor<T extends AsyncTask, F extends ListenableFuture> {

    F submit(T task);

}
