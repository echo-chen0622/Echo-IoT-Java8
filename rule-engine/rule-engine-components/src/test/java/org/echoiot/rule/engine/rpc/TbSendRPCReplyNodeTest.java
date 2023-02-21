package org.echoiot.rule.engine.rpc;

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
import org.echoiot.rule.engine.api.RuleEngineRpcService;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgDataType;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.common.msg.session.SessionMsgType;
import org.echoiot.server.dao.edge.EdgeEventService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TbSendRPCReplyNodeTest {

    private static final String DUMMY_SERVICE_ID = "testServiceId";
    private static final int DUMMY_REQUEST_ID = 0;
    private static final UUID DUMMY_SESSION_ID = UUID.randomUUID();
    private static final String DUMMY_DATA = "{\"key\":\"value\"}";

    TbSendRPCReplyNode node;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());

    @Mock
    private TbContext ctx;

    @Mock
    private RuleEngineRpcService rpcService;

    @Mock
    private EdgeEventService edgeEventService;

    @Mock
    private ListeningExecutor listeningExecutor;

    @Before
    public void setUp() throws TbNodeException {
        node = new TbSendRPCReplyNode();
        @NotNull TbSendRpcReplyNodeConfiguration config = new TbSendRpcReplyNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    @Test
    public void sendReplyToTransport() {
        Mockito.when(ctx.getRpcService()).thenReturn(rpcService);


        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, getDefaultMetadata(),
                                          TbMsgDataType.JSON, DUMMY_DATA, null, null);

        node.onMsg(ctx, msg);

        verify(rpcService).sendRpcReplyToDevice(DUMMY_SERVICE_ID, DUMMY_SESSION_ID, DUMMY_REQUEST_ID, DUMMY_DATA);
        verify(edgeEventService, never()).saveAsync(any());
    }

    @Test
    public void sendReplyToEdgeQueue() {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeEventService()).thenReturn(edgeEventService);
        Mockito.when(edgeEventService.saveAsync(any())).thenReturn(SettableFuture.create());
        Mockito.when(ctx.getDbCallbackExecutor()).thenReturn(listeningExecutor);

        @NotNull TbMsgMetaData defaultMetadata = getDefaultMetadata();
        defaultMetadata.putValue(DataConstants.EDGE_ID, UUID.randomUUID().toString());
        defaultMetadata.putValue(DataConstants.DEVICE_ID, UUID.randomUUID().toString());
        @NotNull TbMsg msg = TbMsg.newMsg(SessionMsgType.POST_TELEMETRY_REQUEST.name(), deviceId, defaultMetadata,
                                          TbMsgDataType.JSON, DUMMY_DATA, null, null);

        node.onMsg(ctx, msg);

        verify(edgeEventService).saveAsync(any());
        verify(rpcService, never()).sendRpcReplyToDevice(DUMMY_SERVICE_ID, DUMMY_SESSION_ID, DUMMY_REQUEST_ID, DUMMY_DATA);
    }

    @NotNull
    private TbMsgMetaData getDefaultMetadata() {
        @NotNull TbSendRpcReplyNodeConfiguration config = new TbSendRpcReplyNodeConfiguration().defaultConfiguration();
        @NotNull TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue(config.getServiceIdMetaDataAttribute(), DUMMY_SERVICE_ID);
        metadata.putValue(config.getSessionIdMetaDataAttribute(), DUMMY_SESSION_ID.toString());
        metadata.putValue(config.getRequestIdMetaDataAttribute(), Integer.toString(DUMMY_REQUEST_ID));
        return metadata;
    }
}
