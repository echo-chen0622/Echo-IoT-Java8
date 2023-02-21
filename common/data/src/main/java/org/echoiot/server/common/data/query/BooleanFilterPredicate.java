package org.echoiot.server.common.data.query;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class BooleanFilterPredicate implements SimpleKeyFilterPredicate<Boolean> {

    private BooleanOperation operation;
    private FilterPredicateValue<Boolean> value;

    @NotNull
    @Override
    public FilterPredicateType getType() {
        return FilterPredicateType.BOOLEAN;
    }

    public enum BooleanOperation {
        EQUAL,
        NOT_EQUAL
    }
}
