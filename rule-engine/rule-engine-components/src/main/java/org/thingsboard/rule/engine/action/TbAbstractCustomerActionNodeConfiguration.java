package org.thingsboard.rule.engine.action;

import lombok.Data;

@Data
public abstract class TbAbstractCustomerActionNodeConfiguration {

    private String customerNamePattern;
    private long customerCacheExpiration;

}
