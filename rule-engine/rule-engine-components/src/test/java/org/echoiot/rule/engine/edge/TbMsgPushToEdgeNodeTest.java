package org.echoiot.rule.engine.edge;

import com.google.common.util.concurrent.SettableFuture;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.common.util.ListeningExecutor;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EdgeId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgDataType;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.common.msg.session.SessionMsgType;
import org.echoiot.server.dao.edge.EdgeEventService;
import org.echoiot.server.dao.edge.EdgeService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TbMsgPushToEdgeNodeTest {

    TbMsgPushToEdgeNode node;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());

    @Mock
    private TbContext ctx;

    @Mock
    private EdgeService edgeService;
    @Mock
    private EdgeEventService edgeEventService;
    @Mock
    private ListeningExecutor dbCallbackExecutor;

    @Before
    public void setUp() throws TbNodeException {
        node = new TbMsgPushToEdgeNode();
        @NotNull TbMsgPushToEdgeNodeConfiguration config = new TbMsgPushToEdgeNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    @Test
    public void ackMsgInCaseNoEdgeRelated() {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeService()).thenReturn(edgeService);
        Mockito.when(edgeService.findRelatedEdgeIdsByEntityId(tenantId, deviceId, new PageLink(TbMsgPushToEdgeNode.DEFAULT_PAGE_SIZE))).thenReturn(new PageData<>());

        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, "{}", null, null);

        node.onMsg(ctx, msg);

        verify(ctx).ack(msg);
    }

    @Test
    public void testAttributeUpdateMsg_userEntity() {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeService()).thenReturn(edgeService);
        Mockito.when(ctx.getEdgeEventService()).thenReturn(edgeEventService);
        Mockito.when(ctx.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        Mockito.when(edgeEventService.saveAsync(any())).thenReturn(SettableFuture.create());

        @NotNull UserId userId = new UserId(UUID.randomUUID());
        @NotNull EdgeId edgeId = new EdgeId(UUID.randomUUID());
        @NotNull PageData<EdgeId> edgePageData = new PageData<>(List.of(edgeId), 1, 1, false);
        Mockito.when(edgeService.findRelatedEdgeIdsByEntityId(tenantId, userId, new PageLink(TbMsgPushToEdgeNode.DEFAULT_PAGE_SIZE))).thenReturn(edgePageData);

        @NotNull TbMsg msg = TbMsg.newMsg(DataConstants.ATTRIBUTES_UPDATED, userId, new TbMsgMetaData(),
                                          TbMsgDataType.JSON, "{}", null, null);

        node.onMsg(ctx, msg);

        verify(edgeEventService).saveAsync(any());
    }
}
