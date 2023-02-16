package org.echoiot.server.service.entitiy;

import org.echoiot.server.common.data.User;

public interface SimpleTbEntityService<T> {

    default T save(T entity) throws Exception {
        return save(entity, null);
    }

    T save(T entity, User user) throws Exception;

    void delete(T entity, User user);

}
