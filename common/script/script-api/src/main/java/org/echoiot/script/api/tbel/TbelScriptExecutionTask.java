package org.echoiot.script.api.tbel;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.script.api.TbScriptExecutionTask;
import org.mvel2.ExecutionContext;


public class TbelScriptExecutionTask extends TbScriptExecutionTask {

    private final ExecutionContext context;

    public TbelScriptExecutionTask(ExecutionContext context, ListenableFuture<Object> resultFuture) {
        super(resultFuture);
        this.context = context;
    }

    @Override
    public void stop(){
        context.stop();
    }
}
