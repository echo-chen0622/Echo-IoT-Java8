package org.thingsboard.server.common.data.kv;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Andrew Shvayka
 */
@Data
public class AttributeKey implements Serializable {
    private final String scope;
    private final String attributeKey;
}
