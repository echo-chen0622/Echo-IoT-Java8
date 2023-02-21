package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.util.mapping.JsonStringType;
import org.hibernate.annotations.TypeDef;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.ENTITY_VIEW_TABLE_FAMILY_NAME)
public class EntityViewEntity extends AbstractEntityViewEntity<EntityView> {

    public EntityViewEntity() {
        super();
    }

    public EntityViewEntity(@NotNull EntityView entityView) {
        super(entityView);
    }

    @Override
    public EntityView toData() {
        return super.toEntityView();
    }
}
