package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
@AllArgsConstructor
public class EdgeDataValidator extends DataValidator<Edge> {

    private final EdgeDao edgeDao;
    private final TenantService tenantService;
    private final CustomerDao customerDao;

    @Override
    protected void validateCreate(TenantId tenantId, Edge edge) {
    }

    @Override
    protected Edge validateUpdate(TenantId tenantId, Edge edge) {
        return edgeDao.findById(edge.getTenantId(), edge.getId().getId());
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Edge edge) {
        if (StringUtils.isEmpty(edge.getType())) {
            throw new DataValidationException("Edge type should be specified!");
        }
        if (StringUtils.isEmpty(edge.getName())) {
            throw new DataValidationException("Edge name should be specified!");
        }
        if (StringUtils.isEmpty(edge.getSecret())) {
            throw new DataValidationException("Edge secret should be specified!");
        }
        if (StringUtils.isEmpty(edge.getRoutingKey())) {
            throw new DataValidationException("Edge routing key should be specified!");
        }
        if (edge.getTenantId() == null) {
            throw new DataValidationException("Edge should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(edge.getTenantId())) {
                throw new DataValidationException("Edge is referencing to non-existent tenant!");
            }
        }
        if (edge.getCustomerId() == null) {
            edge.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!edge.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(edge.getTenantId(), edge.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign edge to non-existent customer!");
            }
            if (!customer.getTenantId().getId().equals(edge.getTenantId().getId())) {
                throw new DataValidationException("Can't assign edge to customer from different tenant!");
            }
        }
    }
}
