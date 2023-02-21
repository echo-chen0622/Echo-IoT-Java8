package org.echoiot.script.api.js;

import org.echoiot.script.api.ScriptInvokeService;
import org.echoiot.server.common.data.script.ScriptLanguage;
import org.jetbrains.annotations.NotNull;

public interface JsInvokeService extends ScriptInvokeService {

    @NotNull
    @Override
    default ScriptLanguage getLanguage() {
        return ScriptLanguage.JS;
    }

}
