package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.widget.BaseWidgetType;
import org.echoiot.server.common.data.widget.WidgetTypeInfo;

@Data
@EqualsAndHashCode(callSuper = true)
public final class WidgetTypeInfoEntity extends AbstractWidgetTypeEntity<WidgetTypeInfo> {

    private String image;
    private String description;
    private String widgetType;

    public WidgetTypeInfoEntity() {
        super();
    }

    public WidgetTypeInfoEntity(WidgetTypeDetailsEntity widgetTypeDetailsEntity) {
        super(widgetTypeDetailsEntity);
        this.image = widgetTypeDetailsEntity.getImage();
        this.description = widgetTypeDetailsEntity.getDescription();
        if (widgetTypeDetailsEntity.getDescriptor() != null && widgetTypeDetailsEntity.getDescriptor().has("type")) {
            this.widgetType = widgetTypeDetailsEntity.getDescriptor().get("type").asText();
        } else {
            this.widgetType = "";
        }
    }

    @Override
    public WidgetTypeInfo toData() {
        BaseWidgetType baseWidgetType = super.toBaseWidgetType();
        WidgetTypeInfo widgetTypeInfo = new WidgetTypeInfo(baseWidgetType);
        widgetTypeInfo.setImage(image);
        widgetTypeInfo.setDescription(description);
        widgetTypeInfo.setWidgetType(widgetType);
        return widgetTypeInfo;
    }

}
