package org.thingsboard.server.dao.model;

public interface SearchTextEntity<D> extends BaseEntity<D> {

    String getSearchTextSource();

    void setSearchText(String searchText);

}
