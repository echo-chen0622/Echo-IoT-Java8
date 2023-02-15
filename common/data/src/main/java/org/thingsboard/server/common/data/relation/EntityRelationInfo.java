package org.thingsboard.server.common.data.relation;

import io.swagger.annotations.ApiModelProperty;

public class EntityRelationInfo extends EntityRelation {

    private static final long serialVersionUID = 2807343097519543363L;

    private String fromName;
    private String toName;

    public EntityRelationInfo() {
        super();
    }

    public EntityRelationInfo(EntityRelation entityRelation) {
        super(entityRelation);
    }

    @ApiModelProperty(position = 6, value = "Name of the entity for [from] direction.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "A4B72CCDFF33")
    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    @ApiModelProperty(position = 7, value = "Name of the entity for [to] direction.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "A4B72CCDFF35")
    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        EntityRelationInfo that = (EntityRelationInfo) o;

        return toName != null ? toName.equals(that.toName) : that.toName == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (toName != null ? toName.hashCode() : 0);
        return result;
    }
}
