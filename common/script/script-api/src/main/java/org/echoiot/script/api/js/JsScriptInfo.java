package org.echoiot.script.api.js;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class JsScriptInfo {

    @NotNull
    private final String hash;
    @NotNull
    private final String functionName;

}
