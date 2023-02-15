package org.thingsboard.server.service.entitiy.user;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

import javax.servlet.http.HttpServletRequest;

public interface TbUserService {
    User save(TenantId tenantId, CustomerId customerId, User tbUser, boolean sendActivationMail, HttpServletRequest request, User user) throws ThingsboardException;

    void delete(TenantId tenantId, CustomerId customerId, User tbUser, User user) throws ThingsboardException;
}
