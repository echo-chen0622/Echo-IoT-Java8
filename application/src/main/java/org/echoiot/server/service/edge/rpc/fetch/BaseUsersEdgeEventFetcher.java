package org.echoiot.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.user.UserService;

@Slf4j
@AllArgsConstructor
public abstract class BaseUsersEdgeEventFetcher extends BasePageableEdgeEventFetcher<User> {

    protected final UserService userService;

    @Override
    PageData<User> fetchPageData(TenantId tenantId, Edge edge, PageLink pageLink) {
        return findUsers(tenantId, pageLink);
    }

    @Override
    EdgeEvent constructEdgeEvent(TenantId tenantId, Edge edge, User user) {
        return EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.USER,
                                            EdgeEventActionType.ADDED, user.getId(), null);
    }

    protected abstract PageData<User> findUsers(TenantId tenantId, PageLink pageLink);
}