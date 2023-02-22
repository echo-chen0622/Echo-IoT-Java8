package org.echoiot.server.common.data.relation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.validation.Length;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

@Slf4j
@ApiModel
public class EntityRelation implements Serializable {

    private static final long serialVersionUID = 2807343040519543363L;

    public static final String EDGE_TYPE = "ManagedByEdge";
    public static final String CONTAINS_TYPE = "Contains";
    public static final String MANAGES_TYPE = "Manages";

    private EntityId from;
    private EntityId to;
    @Length(fieldName = "type")
    private String type;
    private RelationTypeGroup typeGroup;
    @Nullable
    private transient JsonNode additionalInfo;
    @JsonIgnore
    private byte[] additionalInfoBytes;

    public EntityRelation() {
        super();
    }

    public EntityRelation(EntityId from, EntityId to, String type) {
        this(from, to, type, RelationTypeGroup.COMMON);
    }

    public EntityRelation(EntityId from, EntityId to, String type, RelationTypeGroup typeGroup) {
        this(from, to, type, typeGroup, null);
    }

    public EntityRelation(EntityId from, EntityId to, String type, RelationTypeGroup typeGroup, JsonNode additionalInfo) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.typeGroup = typeGroup;
        this.additionalInfo = additionalInfo;
    }

    public EntityRelation(EntityRelation entityRelation) {
        this.from = entityRelation.getFrom();
        this.to = entityRelation.getTo();
        this.type = entityRelation.getType();
        this.typeGroup = entityRelation.getTypeGroup();
        this.additionalInfo = entityRelation.getAdditionalInfo();
    }

    @ApiModelProperty(position = 1, value = "JSON object with [from] Entity Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public EntityId getFrom() {
        return from;
    }

    public void setFrom(EntityId from) {
        this.from = from;
    }

    @ApiModelProperty(position = 2, value = "JSON object with [to] Entity Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public EntityId getTo() {
        return to;
    }

    public void setTo(EntityId to) {
        this.to = to;
    }

    @ApiModelProperty(position = 3, value = "String value of relation type.", example = "Contains")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ApiModelProperty(position = 4, value = "Represents the type group of the relation.", example = "COMMON")
    public RelationTypeGroup getTypeGroup() {
        return typeGroup;
    }

    public void setTypeGroup(RelationTypeGroup typeGroup) {
        this.typeGroup = typeGroup;
    }

    @Nullable
    @ApiModelProperty(position = 5, value = "Additional parameters of the relation", dataType = "com.fasterxml.jackson.databind.JsonNode")
    public JsonNode getAdditionalInfo() {
        return SearchTextBasedWithAdditionalInfo.getJson(() -> additionalInfo, () -> additionalInfoBytes);
    }

    public void setAdditionalInfo(JsonNode addInfo) {
        SearchTextBasedWithAdditionalInfo.setJson(addInfo, json -> this.additionalInfo = json, bytes -> this.additionalInfoBytes = bytes);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntityRelation that = (EntityRelation) o;

        if (!Objects.equals(from, that.from)) return false;
        if (!Objects.equals(to, that.to)) return false;
        if (!Objects.equals(type, that.type)) return false;
        return typeGroup == that.typeGroup;
    }

    @Override
    public int hashCode() {
        int result = from != null ? from.hashCode() : 0;
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (typeGroup != null ? typeGroup.hashCode() : 0);
        return result;
    }
}
