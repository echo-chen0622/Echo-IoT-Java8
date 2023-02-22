package org.echoiot.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.widget.BaseWidgetType;
import org.echoiot.server.common.data.widget.WidgetType;
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
public final class WidgetTypeEntity extends AbstractWidgetTypeEntity<WidgetType> {

    @Type(type="json")
    @Column(name = ModelConstants.WIDGET_TYPE_DESCRIPTOR_PROPERTY)
    private JsonNode descriptor;

    public WidgetTypeEntity() {
        super();
    }

    @Override
    public WidgetType toData() {
        BaseWidgetType baseWidgetType = super.toBaseWidgetType();
        WidgetType widgetType = new WidgetType(baseWidgetType);
        widgetType.setDescriptor(descriptor);
        return widgetType;
    }

}
