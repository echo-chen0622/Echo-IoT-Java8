package org.echoiot.server.common.data;

import org.echoiot.server.common.data.id.UUIDBased;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;

public abstract class SearchTextBased<I extends UUIDBased> extends BaseData<I> {

    private static final long serialVersionUID = -539812997348227609L;

    public SearchTextBased() {
        super();
    }

    public SearchTextBased(I id) {
        super(id);
    }

    public SearchTextBased(@NotNull SearchTextBased<I> searchTextBased) {
        super(searchTextBased);
    }

    @JsonIgnore
    public abstract String getSearchText();

}
