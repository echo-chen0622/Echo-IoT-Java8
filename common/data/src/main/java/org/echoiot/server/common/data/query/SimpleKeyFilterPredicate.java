package org.echoiot.server.common.data.query;

public interface SimpleKeyFilterPredicate<T> extends KeyFilterPredicate {

    FilterPredicateValue<T> getValue();

}
