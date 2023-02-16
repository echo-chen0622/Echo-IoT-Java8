package org.echoiot.server.service.edge.rpc.fetch;

import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.user.UserService;

public class TenantAdminUsersEdgeEventFetcher extends BaseUsersEdgeEventFetcher {

    public TenantAdminUsersEdgeEventFetcher(UserService userService) {
        super(userService);
    }

    @Override
    protected PageData<User> findUsers(TenantId tenantId, PageLink pageLink) {
        return userService.findTenantAdmins(tenantId, pageLink);
    }
}
