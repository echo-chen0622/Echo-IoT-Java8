package org.echoiot.script.api.js;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.script.api.TbScriptExecutionTask;

public class JsScriptExecutionTask extends TbScriptExecutionTask {

    public JsScriptExecutionTask(ListenableFuture<Object> resultFuture) {
        super(resultFuture);
    }

    @Override
    public void stop() {
        // do nothing
    }
}
