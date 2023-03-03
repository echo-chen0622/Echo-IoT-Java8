package org.echoiot.server.common.data.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;
import org.echoiot.server.common.data.SearchTextBased;
import org.echoiot.server.common.data.id.ComponentDescriptorId;
import org.echoiot.server.common.data.validation.Length;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 组件描述类，用于描述组件的基本信息。
 * 与 {@link org.echoiot.server.dao.model.sql.ComponentDescriptorEntity} 相比，这个类没有与数据库交互的字段。是用于描述组件的实体
 *
 * @author Echo
 */
@ApiModel
@ToString
@Data
public class ComponentDescriptor extends SearchTextBased<ComponentDescriptorId> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(position = 3, value = "规则节点类型", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private ComponentType type;
    @ApiModelProperty(position = 4, value = "规则节点的作用域。始终设置为“TENANT”租户，因为“SYSTEM”系统级别还没有规则链。", accessMode = ApiModelProperty.AccessMode.READ_ONLY, allowableValues = "TENANT", example = "TENANT")
    private ComponentScope scope;
    @Length(fieldName = "name")
    @ApiModelProperty(position = 5, value = "规则节点的名称。取自@RuleNode注解", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "自定义规则节点")
    private String name;
    @ApiModelProperty(position = 6, value = "实现规则引擎节点接口的 Java 类的全名", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "com.mycompany.CustomRuleNode")
    private String clazz;
    @ApiModelProperty(position = 7, value = "表示规则节点配置的复杂 JSON 对象", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private transient JsonNode configurationDescriptor;

    public ComponentDescriptor() {
        super();
    }

    public ComponentDescriptor(ComponentDescriptorId id) {
        super(id);
    }

    public ComponentDescriptor(ComponentDescriptor plugin) {
        super(plugin);
        this.type = plugin.getType();
        this.scope = plugin.getScope();
        this.name = plugin.getName();
        this.clazz = plugin.getClazz();
        this.configurationDescriptor = plugin.getConfigurationDescriptor();
    }

    @ApiModelProperty(position = 1, value = "具有描述类 ID 的 JSON 对象。 " + "指定现有描述类 ID 以更新描述类。 " + "引用不存在的描述类 ID 将导致错误。 " + "省略此字段以创建新的描述类")
    @Override
    public ComponentDescriptorId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "描述类创建时间（时间戳，以毫秒为单位）", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    public String getSearchText() {
        return name;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComponentDescriptor that = (ComponentDescriptor) o;

        if (type != that.type) return false;
        if (scope != that.scope) return false;
        if (!Objects.equals(name, that.name)) return false;
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
        return result;
    }

}
