package org.thingsboard.server.common.data.query;

import lombok.Data;

@Data
public class BooleanFilterPredicate implements SimpleKeyFilterPredicate<Boolean> {

    private BooleanOperation operation;
    private FilterPredicateValue<Boolean> value;

    @Override
    public FilterPredicateType getType() {
        return FilterPredicateType.BOOLEAN;
    }

    public enum BooleanOperation {
        EQUAL,
        NOT_EQUAL
    }
}
