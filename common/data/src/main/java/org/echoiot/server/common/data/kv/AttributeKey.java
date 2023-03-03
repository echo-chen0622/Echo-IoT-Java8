package org.echoiot.server.common.data.kv;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Echo
 */
@Data
public class AttributeKey implements Serializable {
    private final String scope;
    private final String attributeKey;
}
