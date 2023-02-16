package org.echoiot.server.dao.service.validator;

import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.springframework.stereotype.Component;

@Component
public class EdgeEventDataValidator extends DataValidator<EdgeEvent> {

    @Override
    protected void validateDataImpl(TenantId tenantId, EdgeEvent edgeEvent) {
        if (edgeEvent.getEdgeId() == null) {
            throw new DataValidationException("Edge id should be specified!");
        }
        if (edgeEvent.getAction() == null) {
            throw new DataValidationException("Edge Event action should be specified!");
        }
    }
}
