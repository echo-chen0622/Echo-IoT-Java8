package org.echoiot.server.dao.sql;

import org.echoiot.server.dao.model.BaseEntity;
import org.echoiot.server.dao.model.SearchTextEntity;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public abstract class JpaAbstractSearchTextDao <E extends BaseEntity<D>, D> extends JpaAbstractDao<E, D> {

    @Override
    protected void setSearchText(E entity) {
        ((SearchTextEntity) entity).setSearchText(((SearchTextEntity) entity).getSearchTextSource().toLowerCase());
    }
}
