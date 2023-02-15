package org.thingsboard.rule.engine.action;

import lombok.Data;

@Data
public abstract class TbAbstractRelationActionNodeConfiguration {

    private String direction;
    private String relationType;

    private String entityType;
    private String entityNamePattern;
    private String entityTypePattern;

    private long entityCacheExpiration;

}
