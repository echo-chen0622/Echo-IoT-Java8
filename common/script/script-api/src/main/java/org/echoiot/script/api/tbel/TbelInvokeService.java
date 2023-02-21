package org.echoiot.script.api.tbel;

import org.echoiot.script.api.ScriptInvokeService;
import org.echoiot.server.common.data.script.ScriptLanguage;
import org.jetbrains.annotations.NotNull;

public interface TbelInvokeService extends ScriptInvokeService {

    @NotNull
    @Override
    default ScriptLanguage getLanguage() {
        return ScriptLanguage.TBEL;
    }

}
