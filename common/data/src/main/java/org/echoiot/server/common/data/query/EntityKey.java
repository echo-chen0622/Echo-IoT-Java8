package org.echoiot.server.common.data.query;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@ApiModel
@Data
public class EntityKey implements Serializable {
    private static final long serialVersionUID = -6421575477523085543L;

    @NotNull
    private final EntityKeyType type;
    @NotNull
    private final String key;
}
