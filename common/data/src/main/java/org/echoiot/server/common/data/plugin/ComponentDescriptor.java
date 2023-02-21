package org.echoiot.server.common.data.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.echoiot.server.common.data.SearchTextBased;
import org.echoiot.server.common.data.id.ComponentDescriptorId;
import org.echoiot.server.common.data.validation.Length;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Andrew Shvayka
 */
@ApiModel
@ToString
public class ComponentDescriptor extends SearchTextBased<ComponentDescriptorId> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(position = 3, value = "Type of the Rule Node", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Getter @Setter private ComponentType type;
    @ApiModelProperty(position = 4, value = "Scope of the Rule Node. Always set to 'TENANT', since no rule chains on the 'SYSTEM' level yet.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, allowableValues = "TENANT", example = "TENANT")
    @Getter @Setter private ComponentScope scope;
    @Length(fieldName = "name")
    @ApiModelProperty(position = 5, value = "Name of the Rule Node. Taken from the @RuleNode annotation.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "Custom Rule Node")
    @Getter @Setter private String name;
    @ApiModelProperty(position = 6, value = "Full name of the Java class that implements the Rule Engine Node interface.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "com.mycompany.CustomRuleNode")
    @Getter @Setter private String clazz;
    @ApiModelProperty(position = 7, value = "Complex JSON object that represents the Rule Node configuration.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Getter @Setter private transient JsonNode configurationDescriptor;
    @Length(fieldName = "actions")
    @ApiModelProperty(position = 8, value = "Rule Node Actions. Deprecated. Always null.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Getter @Setter private String actions;

    public ComponentDescriptor() {
        super();
    }

    public ComponentDescriptor(ComponentDescriptorId id) {
        super(id);
    }

    public ComponentDescriptor(@NotNull ComponentDescriptor plugin) {
        super(plugin);
        this.type = plugin.getType();
        this.scope = plugin.getScope();
        this.name = plugin.getName();
        this.clazz = plugin.getClazz();
        this.configurationDescriptor = plugin.getConfigurationDescriptor();
        this.actions = plugin.getActions();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the descriptor Id. " +
            "Specify existing descriptor id to update the descriptor. " +
            "Referencing non-existing descriptor Id will cause error. " +
            "Omit this field to create new descriptor." )
    @Override
    public ComponentDescriptorId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the descriptor creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    public String getSearchText() {
        return name;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        @NotNull ComponentDescriptor that = (ComponentDescriptor) o;

        if (type != that.type) return false;
        if (scope != that.scope) return false;
        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(actions, that.actions)) return false;
        if (!Objects.equals(configurationDescriptor, that.configurationDescriptor)) return false;
        return Objects.equals(clazz, that.clazz);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
        result = 31 * result + (actions != null ? actions.hashCode() : 0);
        return result;
    }

}
