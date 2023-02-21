package org.echoiot.server.common.data.page;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TimePageLink extends PageLink {

    private final Long startTime;
    private final Long endTime;

    public TimePageLink(@NotNull PageLink pageLink, Long startTime, Long endTime) {
        super(pageLink);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public TimePageLink(int pageSize) {
        this(pageSize, 0);
    }

    public TimePageLink(int pageSize, int page) {
        this(pageSize, page, null);
    }

    public TimePageLink(int pageSize, int page, String textSearch) {
        this(pageSize, page, textSearch, null, null, null);
    }

    public TimePageLink(int pageSize, int page, String textSearch, SortOrder sortOrder) {
        this(pageSize, page, textSearch, sortOrder, null, null);
    }

    public TimePageLink(int pageSize, int page, String textSearch, SortOrder sortOrder, Long startTime, Long endTime) {
        super(pageSize, page, textSearch, sortOrder);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @NotNull
    @JsonIgnore
    public TimePageLink nextPageLink() {
        return new TimePageLink(this.getPageSize(), this.getPage()+1, this.getTextSearch(), this.getSortOrder(),
                this.startTime, this.endTime);
    }

    @Override
    public Sort toSort(@Nullable SortOrder sortOrder, @NotNull Map<String,String> columnMap) {
        if (sortOrder == null) {
            return super.toSort(sortOrder, columnMap);
        } else {
            return toSort(new ArrayList<>(List.of(sortOrder)), columnMap);
        }
    }

    @Override
    public Sort toSort(@NotNull List<SortOrder> sortOrders, @NotNull Map<String,String> columnMap) {
        if (!isDefaultSortOrderAvailable(sortOrders)) {
            sortOrders.add(new SortOrder(DEFAULT_SORT_PROPERTY, SortOrder.Direction.ASC));
        }
        return super.toSort(sortOrders, columnMap);
    }

    private boolean isDefaultSortOrderAvailable(@NotNull List<SortOrder> sortOrders) {
        for (@NotNull SortOrder sortOrder : sortOrders) {
            if (DEFAULT_SORT_PROPERTY.equals(sortOrder.getProperty())) {
                return true;
            }
        }
        return false;
    }
}
