package org.thingsboard.server.common.data.page;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApiModel
public class PageData<T> {

    private final List<T> data;
    private final int totalPages;
    private final long totalElements;
    private final boolean hasNext;

    public PageData() {
        this(Collections.emptyList(), 0, 0, false);
    }

    @JsonCreator
    public PageData(@JsonProperty("data") List<T> data,
                    @JsonProperty("totalPages") int totalPages,
                    @JsonProperty("totalElements") long totalElements,
                    @JsonProperty("hasNext") boolean hasNext) {
        this.data = data;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.hasNext = hasNext;
    }

    @ApiModelProperty(position = 1, value = "Array of the entities", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public List<T> getData() {
        return data;
    }

    @ApiModelProperty(position = 2, value = "Total number of available pages. Calculated based on the 'pageSize' request parameter and total number of entities that match search criteria", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public int getTotalPages() {
        return totalPages;
    }

    @ApiModelProperty(position = 3, value = "Total number of elements in all available pages", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public long getTotalElements() {
        return totalElements;
    }

    @ApiModelProperty(position = 4, value = "'false' value indicates the end of the result set", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @JsonProperty("hasNext")
    public boolean hasNext() {
        return hasNext;
    }

    public <D> PageData<D> mapData(Function<T, D> mapper) {
        return new PageData<>(getData().stream().map(mapper).collect(Collectors.toList()), getTotalPages(), getTotalElements(), hasNext());
    }

}
