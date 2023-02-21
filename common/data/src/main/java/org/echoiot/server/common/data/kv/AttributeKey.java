package org.echoiot.server.common.data.kv;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author Andrew Shvayka
 */
@Data
public class AttributeKey implements Serializable {
    @NotNull
    private final String scope;
    @NotNull
    private final String attributeKey;
}
