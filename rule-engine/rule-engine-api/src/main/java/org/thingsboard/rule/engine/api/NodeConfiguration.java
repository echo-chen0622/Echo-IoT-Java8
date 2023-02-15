package org.thingsboard.rule.engine.api;

public interface NodeConfiguration<T extends NodeConfiguration> {

    T defaultConfiguration();

}
