package org.thingsboard.server.service.entitiy;

import org.thingsboard.server.common.data.User;

public interface SimpleTbEntityService<T> {

    default T save(T entity) throws Exception {
        return save(entity, null);
    }

    T save(T entity, User user) throws Exception;

    void delete(T entity, User user);

}
