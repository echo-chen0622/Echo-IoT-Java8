package org.echoiot.server.common.data.query;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel
@Data
public class EntityKey implements Serializable {
    private static final long serialVersionUID = -6421575477523085543L;

    private final EntityKeyType type;
    private final String key;
}
