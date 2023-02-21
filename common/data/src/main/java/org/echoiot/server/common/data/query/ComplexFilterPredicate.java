package org.echoiot.server.common.data.query;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
public class ComplexFilterPredicate implements KeyFilterPredicate {

    private ComplexOperation operation;
    private List<KeyFilterPredicate> predicates;

    @NotNull
    @Override
    public FilterPredicateType getType() {
        return FilterPredicateType.COMPLEX;
    }

    public enum ComplexOperation {
        AND,
        OR
    }
}
