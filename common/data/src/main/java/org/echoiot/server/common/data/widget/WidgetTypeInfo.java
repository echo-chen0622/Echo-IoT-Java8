package org.echoiot.server.common.data.widget;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.echoiot.server.common.data.validation.NoXss;
import org.echoiot.server.common.data.id.WidgetTypeId;
import org.jetbrains.annotations.NotNull;

@Data
public class WidgetTypeInfo extends BaseWidgetType {

    @ApiModelProperty(position = 7, value = "Base64 encoded widget thumbnail", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String image;
    @NoXss
    @ApiModelProperty(position = 7, value = "Description of the widget type", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String description;
    @NoXss
    @ApiModelProperty(position = 8, value = "Type of the widget (timeseries, latest, control, alarm or static)", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String widgetType;

    public WidgetTypeInfo() {
        super();
    }

    public WidgetTypeInfo(WidgetTypeId id) {
        super(id);
    }

    public WidgetTypeInfo(@NotNull BaseWidgetType baseWidgetType) {
        super(baseWidgetType);
    }

    public WidgetTypeInfo(@NotNull WidgetTypeInfo widgetTypeInfo) {
        super(widgetTypeInfo);
        this.image = widgetTypeInfo.getImage();
        this.description = widgetTypeInfo.getDescription();
        this.widgetType = widgetTypeInfo.getWidgetType();
    }
}
