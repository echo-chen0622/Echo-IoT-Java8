package org.echoiot.rule.engine.api;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class EmptyNodeConfiguration implements NodeConfiguration<EmptyNodeConfiguration> {

    private int version;

    @NotNull
    @Override
    public EmptyNodeConfiguration defaultConfiguration() {
        @NotNull EmptyNodeConfiguration configuration = new EmptyNodeConfiguration();
        return configuration;
    }
}
