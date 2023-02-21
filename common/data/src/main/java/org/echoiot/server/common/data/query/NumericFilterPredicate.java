package org.echoiot.server.common.data.query;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class NumericFilterPredicate implements SimpleKeyFilterPredicate<Double>  {

    private NumericOperation operation;
    private FilterPredicateValue<Double> value;

    @NotNull
    @Override
    public FilterPredicateType getType() {
        return FilterPredicateType.NUMERIC;
    }

    public enum NumericOperation {
        EQUAL,
        NOT_EQUAL,
        GREATER,
        LESS,
        GREATER_OR_EQUAL,
        LESS_OR_EQUAL
    }
}
