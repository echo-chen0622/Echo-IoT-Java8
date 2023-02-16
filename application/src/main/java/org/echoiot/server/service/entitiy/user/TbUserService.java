package org.echoiot.server.service.entitiy.user;

import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;

import javax.servlet.http.HttpServletRequest;

public interface TbUserService {
    User save(TenantId tenantId, CustomerId customerId, User tbUser, boolean sendActivationMail, HttpServletRequest request, User user) throws EchoiotException;

    void delete(TenantId tenantId, CustomerId customerId, User tbUser, User user) throws EchoiotException;
}
