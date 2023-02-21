package org.echoiot.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

/**
 * Created by ashvayka on 19.01.18.
 */
@Data
public final class TbNodeConfiguration {

    @NotNull
    private final JsonNode data;

}
