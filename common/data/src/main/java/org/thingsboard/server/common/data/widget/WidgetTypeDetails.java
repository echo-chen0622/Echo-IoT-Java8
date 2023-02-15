package org.thingsboard.server.common.data.widget;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Data
@JsonPropertyOrder({ "alias", "name", "image", "description", "descriptor" })
public class WidgetTypeDetails extends WidgetType {

    @Length(fieldName = "image", max = 1000000)
    @ApiModelProperty(position = 8, value = "Base64 encoded thumbnail", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String image;
    @NoXss
    @Length(fieldName = "description")
    @ApiModelProperty(position = 9, value = "Description of the widget", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String description;

    public WidgetTypeDetails() {
        super();
    }

    public WidgetTypeDetails(WidgetTypeId id) {
        super(id);
    }

    public WidgetTypeDetails(BaseWidgetType baseWidgetType) {
        super(baseWidgetType);
    }

    public WidgetTypeDetails(WidgetTypeDetails widgetTypeDetails) {
        super(widgetTypeDetails);
        this.image = widgetTypeDetails.getImage();
        this.description = widgetTypeDetails.getDescription();
    }
}
