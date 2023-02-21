package org.echoiot.rule.engine.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import lombok.Data;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Created by Echo on 02.04.18.
 */
@Data
class TelemetryNodeCallback implements FutureCallback<Void> {
    @NotNull
    private final TbContext ctx;
    @NotNull
    private final TbMsg msg;

    @Override
    public void onSuccess(@Nullable Void result) {
        ctx.tellSuccess(msg);
    }

    @Override
    public void onFailure(Throwable t) {
        ctx.tellFailure(msg, t);
    }
}
