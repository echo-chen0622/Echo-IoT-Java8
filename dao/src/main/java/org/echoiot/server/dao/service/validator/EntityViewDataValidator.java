package org.echoiot.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.EntityView;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.customer.CustomerDao;
import org.echoiot.server.dao.entityview.EntityViewDao;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.tenant.TenantService;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class EntityViewDataValidator extends DataValidator<EntityView> {

    private final EntityViewDao entityViewDao;
    private final TenantService tenantService;
    private final CustomerDao customerDao;

    @Override
    protected void validateCreate(TenantId tenantId, EntityView entityView) {
        entityViewDao.findEntityViewByTenantIdAndName(entityView.getTenantId().getId(), entityView.getName())
                .ifPresent(e -> {
                    throw new DataValidationException("Entity view with such name already exists!");
                });
    }

    @Nullable
    @Override
    protected EntityView validateUpdate(TenantId tenantId, EntityView entityView) {
        var opt = entityViewDao.findEntityViewByTenantIdAndName(entityView.getTenantId().getId(), entityView.getName());
        opt.ifPresent(e -> {
            if (!e.getUuidId().equals(entityView.getUuidId())) {
                throw new DataValidationException("Entity view with such name already exists!");
            }
        });
        return opt.orElse(null);
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, EntityView entityView) {
        if (StringUtils.isEmpty(entityView.getType())) {
            throw new DataValidationException("Entity View type should be specified!");
        }
        if (StringUtils.isEmpty(entityView.getName())) {
            throw new DataValidationException("Entity view name should be specified!");
        }
        if (entityView.getTenantId() == null) {
            throw new DataValidationException("Entity view should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(entityView.getTenantId())) {
                throw new DataValidationException("Entity view is referencing to non-existent tenant!");
            }
        }
        if (entityView.getCustomerId() == null) {
            entityView.setCustomerId(new CustomerId(ModelConstants.NULL_UUID));
        } else if (!entityView.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, entityView.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign entity view to non-existent customer!");
            }
            if (!customer.getTenantId().getId().equals(entityView.getTenantId().getId())) {
                throw new DataValidationException("Can't assign entity view to customer from different tenant!");
            }
        }
    }
}
