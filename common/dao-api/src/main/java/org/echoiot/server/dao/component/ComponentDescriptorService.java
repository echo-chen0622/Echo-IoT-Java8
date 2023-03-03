package org.echoiot.server.dao.component;

import com.fasterxml.jackson.databind.JsonNode;
import org.echoiot.server.common.data.id.ComponentDescriptorId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.plugin.ComponentDescriptor;
import org.echoiot.server.common.data.plugin.ComponentScope;
import org.echoiot.server.common.data.plugin.ComponentType;

/**
 * @author Echo
 */
public interface ComponentDescriptorService {

    /**
     * 保存组件
     *
     * @param tenantId  租户ID
     * @param component 组件
     *
     * @return 组件
     */
    ComponentDescriptor saveComponent(TenantId tenantId, ComponentDescriptor component);

    /**
     * 根据ID查找组件
     *
     * @param tenantId    租户ID
     * @param componentId 组件ID
     *
     * @return 组件
     */
    ComponentDescriptor findById(TenantId tenantId, ComponentDescriptorId componentId);

    /**
     * 根据类名查找组件
     *
     * @param tenantId 租户ID
     * @param clazz    类名
     *
     * @return 组件
     */
    ComponentDescriptor findByClazz(TenantId tenantId, String clazz);

    PageData<ComponentDescriptor> findByTypeAndPageLink(TenantId tenantId, ComponentType type, PageLink pageLink);

    PageData<ComponentDescriptor> findByScopeAndTypeAndPageLink(TenantId tenantId, ComponentScope scope, ComponentType type, PageLink pageLink);

    boolean validate(TenantId tenantId, ComponentDescriptor component, JsonNode configuration);

    void deleteByClazz(TenantId tenantId, String clazz);

}
