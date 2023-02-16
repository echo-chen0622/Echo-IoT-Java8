package org.echoiot.rule.engine.api;

import lombok.Data;

@Data
public class EmptyNodeConfiguration implements NodeConfiguration<EmptyNodeConfiguration> {

    private int version;

    @Override
    public EmptyNodeConfiguration defaultConfiguration() {
        EmptyNodeConfiguration configuration = new EmptyNodeConfiguration();
        return configuration;
    }
}
