package org.thingsboard.server.dao.sql.component;

import org.thingsboard.server.dao.model.sql.ComponentDescriptorEntity;

public interface ComponentDescriptorInsertRepository {

    ComponentDescriptorEntity saveOrUpdate(ComponentDescriptorEntity entity);

}
