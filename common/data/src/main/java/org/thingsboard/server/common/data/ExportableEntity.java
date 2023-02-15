package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModelProperty;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

public interface ExportableEntity<I extends EntityId> extends HasId<I>, HasName {

    void setId(I id);

    @ApiModelProperty(position = 100, value = "JSON object with External Id from the VCS", accessMode = ApiModelProperty.AccessMode.READ_ONLY, hidden = true)
    I getExternalId();

    void setExternalId(I externalId);

    long getCreatedTime();

    void setCreatedTime(long createdTime);

    void setTenantId(TenantId tenantId);

}
