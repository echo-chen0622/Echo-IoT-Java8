package org.thingsboard.server.common.data.id;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.UUID;

@ApiModel
public abstract class UUIDBased implements HasUUID, Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID id;

    public UUIDBased() {
        this(UUID.randomUUID());
    }

    public UUIDBased(UUID id) {
        super();
        this.id = id;
    }

    @ApiModelProperty(position = 1, required = true, value = "string", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    public UUID getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UUIDBased other = (UUIDBased) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return id.toString();
    }

}
