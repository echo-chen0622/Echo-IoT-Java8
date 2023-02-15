package org.thingsboard.script.api.js;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.script.api.TbScriptExecutionTask;

public class JsScriptExecutionTask extends TbScriptExecutionTask {

    public JsScriptExecutionTask(ListenableFuture<Object> resultFuture) {
        super(resultFuture);
    }

    @Override
    public void stop() {
        // do nothing
    }
}
