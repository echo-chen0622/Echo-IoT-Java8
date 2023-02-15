package org.thingsboard.server.common.data.device.credentials.lwm2m;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractLwM2MClientCredential implements LwM2MClientCredential {
    private String endpoint;
}
