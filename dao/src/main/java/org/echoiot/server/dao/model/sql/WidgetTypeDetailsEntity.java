package org.echoiot.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.widget.BaseWidgetType;
import org.echoiot.server.common.data.widget.WidgetTypeDetails;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.util.mapping.JsonStringType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.WIDGET_TYPE_COLUMN_FAMILY_NAME)
public class WidgetTypeDetailsEntity extends AbstractWidgetTypeEntity<WidgetTypeDetails> {

    @Column(name = ModelConstants.WIDGET_TYPE_IMAGE_PROPERTY)
    private String image;

    @Column(name = ModelConstants.WIDGET_TYPE_DESCRIPTION_PROPERTY)
    private String description;

    @Type(type="json")
    @Column(name = ModelConstants.WIDGET_TYPE_DESCRIPTOR_PROPERTY)
    private JsonNode descriptor;

    public WidgetTypeDetailsEntity() {
        super();
    }

    public WidgetTypeDetailsEntity(WidgetTypeDetails widgetTypeDetails) {
        super(widgetTypeDetails);
        this.image = widgetTypeDetails.getImage();
        this.description = widgetTypeDetails.getDescription();
        this.descriptor = widgetTypeDetails.getDescriptor();
    }

    @Override
    public WidgetTypeDetails toData() {
        BaseWidgetType baseWidgetType = super.toBaseWidgetType();
        WidgetTypeDetails widgetTypeDetails = new WidgetTypeDetails(baseWidgetType);
        widgetTypeDetails.setImage(image);
        widgetTypeDetails.setDescription(description);
        widgetTypeDetails.setDescriptor(descriptor);
        return widgetTypeDetails;
    }
}
