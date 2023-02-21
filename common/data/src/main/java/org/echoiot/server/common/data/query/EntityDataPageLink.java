package org.echoiot.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
public class EntityDataPageLink {

    private int pageSize;
    private int page;
    private String textSearch;
    private EntityDataSortOrder sortOrder;
    private boolean dynamic = false;

    public EntityDataPageLink() {
    }

    public EntityDataPageLink(int pageSize, int page, String textSearch, EntityDataSortOrder sortOrder) {
        this(pageSize, page, textSearch, sortOrder, false);
    }

    @NotNull
    @JsonIgnore
    public EntityDataPageLink nextPageLink() {
        return new EntityDataPageLink(this.pageSize, this.page + 1, this.textSearch, this.sortOrder);
    }
}
