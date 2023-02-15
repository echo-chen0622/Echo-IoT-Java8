package org.thingsboard.server.transport.lwm2m.server;

import lombok.Data;
import org.eclipse.leshan.core.model.ResourceModel;

@Data
public class LwM2mOtaConvert {
    private ResourceModel.Type currentType;
    private Object value;
}
