package org.echoiot.server.dao.device.claim;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@AllArgsConstructor
@Data
public class ClaimData implements Serializable {

    private static final long serialVersionUID = -3922621193389915930L;

    @NotNull
    private final String secretKey;
    private final long expirationTime;

}
