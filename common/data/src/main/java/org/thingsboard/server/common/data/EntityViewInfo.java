package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityViewId;

@Data
public class EntityViewInfo extends EntityView {

    @ApiModelProperty(position = 12, value = "Title of the Customer that owns the entity view.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String customerTitle;
    @ApiModelProperty(position = 13, value = "Indicates special 'Public' Customer that is auto-generated to use the entity view on public dashboards.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private boolean customerIsPublic;

    public EntityViewInfo() {
        super();
    }

    public EntityViewInfo(EntityViewId entityViewId) {
        super(entityViewId);
    }

    public EntityViewInfo(EntityView entityView, String customerTitle, boolean customerIsPublic) {
        super(entityView);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
    }
}
