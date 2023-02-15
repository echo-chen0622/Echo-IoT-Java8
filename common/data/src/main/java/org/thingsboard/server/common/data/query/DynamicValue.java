package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

@Data
@RequiredArgsConstructor
public class DynamicValue<T> implements Serializable {

    @JsonIgnore
    private T resolvedValue;

    private final DynamicValueSourceType sourceType;
    @NoXss
    private final String sourceAttribute;
    private final boolean inherit;

    public DynamicValue(DynamicValueSourceType sourceType, String sourceAttribute) {
        this.sourceAttribute = sourceAttribute;
        this.sourceType = sourceType;
        this.inherit = false;
    }

}
