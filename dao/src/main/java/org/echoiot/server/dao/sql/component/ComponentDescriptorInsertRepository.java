package org.echoiot.server.dao.sql.component;

import org.echoiot.server.dao.model.sql.ComponentDescriptorEntity;

public interface ComponentDescriptorInsertRepository {

    ComponentDescriptorEntity saveOrUpdate(ComponentDescriptorEntity entity);

}
