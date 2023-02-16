package org.thingsboard.script.api.js;

import org.thingsboard.script.api.ScriptInvokeService;
import org.echoiot.server.common.data.script.ScriptLanguage;

public interface JsInvokeService extends ScriptInvokeService {

    @Override
    default ScriptLanguage getLanguage() {
        return ScriptLanguage.JS;
    }

}
