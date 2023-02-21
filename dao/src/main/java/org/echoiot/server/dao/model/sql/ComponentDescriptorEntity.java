package org.echoiot.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.echoiot.server.common.data.id.ComponentDescriptorId;
import org.echoiot.server.common.data.plugin.ComponentDescriptor;
import org.echoiot.server.common.data.plugin.ComponentScope;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.dao.model.BaseSqlEntity;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.model.SearchTextEntity;
import org.echoiot.server.dao.util.mapping.JsonStringType;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.COMPONENT_DESCRIPTOR_COLUMN_FAMILY_NAME)
public class ComponentDescriptorEntity extends BaseSqlEntity<ComponentDescriptor> implements SearchTextEntity<ComponentDescriptor> {

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_TYPE_PROPERTY)
    private ComponentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_SCOPE_PROPERTY)
    private ComponentScope scope;

    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_CLASS_PROPERTY, unique = true)
    private String clazz;

    @Type(type = "json")
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_CONFIGURATION_DESCRIPTOR_PROPERTY)
    private JsonNode configurationDescriptor;

    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_ACTIONS_PROPERTY)
    private String actions;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    public ComponentDescriptorEntity() {
    }

    public ComponentDescriptorEntity(@NotNull ComponentDescriptor component) {
        if (component.getId() != null) {
            this.setUuid(component.getId().getId());
        }
        this.setCreatedTime(component.getCreatedTime());
        this.actions = component.getActions();
        this.type = component.getType();
        this.scope = component.getScope();
        this.name = component.getName();
        this.clazz = component.getClazz();
        this.configurationDescriptor = component.getConfigurationDescriptor();
        this.searchText = component.getName();
    }

    @NotNull
    @Override
    public ComponentDescriptor toData() {
        @NotNull ComponentDescriptor data = new ComponentDescriptor(new ComponentDescriptorId(this.getUuid()));
        data.setCreatedTime(createdTime);
        data.setType(type);
        data.setScope(scope);
        data.setName(this.getName());
        data.setClazz(this.getClazz());
        data.setActions(this.getActions());
        data.setConfigurationDescriptor(configurationDescriptor);
        return data;
    }

    public String getSearchText() {
        return searchText;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public String getSearchTextSource() {
        return getSearchText();
    }
}
