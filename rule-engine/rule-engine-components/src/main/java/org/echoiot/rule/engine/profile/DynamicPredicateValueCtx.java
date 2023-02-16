package org.echoiot.rule.engine.profile;

public interface DynamicPredicateValueCtx {

    EntityKeyValue getTenantValue(String key);

    EntityKeyValue getCustomerValue(String key);

    void resetCustomer();
}
