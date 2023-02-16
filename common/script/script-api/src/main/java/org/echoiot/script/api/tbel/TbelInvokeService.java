package org.echoiot.script.api.tbel;

import org.echoiot.script.api.ScriptInvokeService;
import org.echoiot.server.common.data.script.ScriptLanguage;

public interface TbelInvokeService extends ScriptInvokeService {

    @Override
    default ScriptLanguage getLanguage() {
        return ScriptLanguage.TBEL;
    }

}
