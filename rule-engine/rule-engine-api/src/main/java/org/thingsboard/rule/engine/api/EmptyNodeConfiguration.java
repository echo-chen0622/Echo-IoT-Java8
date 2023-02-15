package org.thingsboard.rule.engine.api;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
public class EmptyNodeConfiguration implements NodeConfiguration<EmptyNodeConfiguration> {

    private int version;

    @Override
    public EmptyNodeConfiguration defaultConfiguration() {
        EmptyNodeConfiguration configuration = new EmptyNodeConfiguration();
        return configuration;
    }
}
