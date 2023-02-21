package org.echoiot.server.common.data;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class ClaimRequest {

    @NotNull
    private final String secretKey;

}
