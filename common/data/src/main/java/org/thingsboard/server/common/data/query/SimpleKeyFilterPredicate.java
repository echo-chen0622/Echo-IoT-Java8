package org.thingsboard.server.common.data.query;

public interface SimpleKeyFilterPredicate<T> extends KeyFilterPredicate {

    FilterPredicateValue<T> getValue();

}
