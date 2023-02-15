package org.thingsboard.server.common.data.query;

import lombok.Data;

import javax.validation.Valid;

@Data
public class StringFilterPredicate implements SimpleKeyFilterPredicate<String> {

    private StringOperation operation;
    @Valid
    private FilterPredicateValue<String> value;
    private boolean ignoreCase;

    @Override
    public FilterPredicateType getType() {
        return FilterPredicateType.STRING;
    }

    public enum StringOperation {
        EQUAL,
        NOT_EQUAL,
        STARTS_WITH,
        ENDS_WITH,
        CONTAINS,
        NOT_CONTAINS,
        IN,
        NOT_IN
    }
}
