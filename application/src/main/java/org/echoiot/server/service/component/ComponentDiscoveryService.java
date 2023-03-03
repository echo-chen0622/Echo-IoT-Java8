package org.echoiot.server.service.component;

import org.echoiot.server.common.data.plugin.ComponentDescriptor;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.data.rule.RuleChainType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Echo
 */
public interface ComponentDiscoveryService {

    void discoverComponents();

    List<ComponentDescriptor> getComponents(ComponentType type, RuleChainType ruleChainType);

    List<ComponentDescriptor> getComponents(Set<ComponentType> types, RuleChainType ruleChainType);

    Optional<ComponentDescriptor> getComponent(String clazz);
}
