package org.echoiot.script.api.tbel;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Data
public class TbelScript {

    @NotNull
    private final String scriptBody;
    @NotNull
    private final String[] argNames;

    @NotNull
    public Map createVars(@NotNull Object[] args) {
        if (args == null || args.length != argNames.length) {
            throw new IllegalArgumentException("Invalid number of argument values");
        }
        @NotNull var result = new HashMap<>();
        for (int i = 0; i < argNames.length; i++) {
            result.put(argNames[i], args[i]);
        }
        return result;
    }
}
