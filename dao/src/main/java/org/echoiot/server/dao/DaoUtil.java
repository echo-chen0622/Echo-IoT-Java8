package org.echoiot.server.dao;

import org.echoiot.server.common.data.id.UUIDBased;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.page.SortOrder;
import org.echoiot.server.dao.model.ToData;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class DaoUtil {

    private DaoUtil() {
    }

    public static <T> PageData<T> toPageData(Page<? extends ToData<T>> page) {
        List<T> data = convertDataList(page.getContent());
        return new PageData<>(data, page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    public static <T> PageData<T> pageToPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    public static Pageable toPageable(PageLink pageLink) {
        return toPageable(pageLink, Collections.emptyMap());
    }

    public static Pageable toPageable(PageLink pageLink, Map<String, String> columnMap) {
        return PageRequest.of(pageLink.getPage(), pageLink.getPageSize(), pageLink.toSort(pageLink.getSortOrder(), columnMap));
    }

    public static Pageable toPageable(PageLink pageLink, List<SortOrder> sortOrders) {
        return toPageable(pageLink, Collections.emptyMap(), sortOrders);
    }

    public static Pageable toPageable(PageLink pageLink, Map<String, String> columnMap, List<SortOrder> sortOrders) {
        return PageRequest.of(pageLink.getPage(), pageLink.getPageSize(), pageLink.toSort(sortOrders, columnMap));
    }

    public static <T> List<T> convertDataList(@Nullable Collection<? extends ToData<T>> toDataList) {
        List<T> list = Collections.emptyList();
        if (toDataList != null && !toDataList.isEmpty()) {
            list = new ArrayList<>();
            for (@Nullable ToData<T> object : toDataList) {
                if (object != null) {
                    list.add(object.toData());
                }
            }
        }
        return list;
    }

    @Nullable
    public static <T> T getData(@Nullable ToData<T> data) {
        @Nullable T object = null;
        if (data != null) {
            object = data.toData();
        }
        return object;
    }

    @Nullable
    public static <T> T getData(Optional<? extends ToData<T>> data) {
        @Nullable T object = null;
        if (data.isPresent()) {
            object = data.get().toData();
        }
        return object;
    }

    @Nullable
    public static UUID getId(@Nullable UUIDBased idBased) {
        @Nullable UUID id = null;
        if (idBased != null) {
            id = idBased.getId();
        }
        return id;
    }

    public static List<UUID> toUUIDs(List<? extends UUIDBased> idBasedIds) {
        List<UUID> ids = new ArrayList<>();
        for (UUIDBased idBased : idBasedIds) {
            ids.add(getId(idBased));
        }
        return ids;
    }

    public static <T> void processInBatches(Function<PageLink, PageData<T>> finder, int batchSize, Consumer<T> processor) {
        PageLink pageLink = new PageLink(batchSize);
        PageData<T> batch;

        boolean hasNextBatch;
        do {
            batch = finder.apply(pageLink);
            batch.getData().forEach(processor);

            hasNextBatch = batch.hasNext();
            pageLink = pageLink.nextPageLink();
        } while (hasNextBatch);
    }

}
