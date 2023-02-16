package org.echoiot.server.service.edge.rpc.fetch;

import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.user.UserService;

public class CustomerUsersEdgeEventFetcher extends BaseUsersEdgeEventFetcher {

    private final CustomerId customerId;

    public CustomerUsersEdgeEventFetcher(UserService userService, CustomerId customerId) {
        super(userService);
        this.customerId = customerId;
    }

    @Override
    protected PageData<User> findUsers(TenantId tenantId, PageLink pageLink) {
        return userService.findCustomerUsers(tenantId, customerId, pageLink);
    }
}
