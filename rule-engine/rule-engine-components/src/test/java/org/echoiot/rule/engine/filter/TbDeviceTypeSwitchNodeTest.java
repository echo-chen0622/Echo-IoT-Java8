package org.echoiot.rule.engine.filter;

import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.*;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.common.msg.queue.TbMsgCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TbDeviceTypeSwitchNodeTest {

    TenantId tenantId;
    DeviceId deviceId;
    DeviceId deviceIdDeleted;
    DeviceProfile deviceProfile;
    TbContext ctx;
    TbDeviceTypeSwitchNode node;
    EmptyNodeConfiguration config;
    TbMsgCallback callback;
    RuleEngineDeviceProfileCache deviceProfileCache;

    @BeforeEach
    void setUp() throws TbNodeException {
        tenantId = new TenantId(UUID.randomUUID());
        deviceId = new DeviceId(UUID.randomUUID());
        deviceIdDeleted = new DeviceId(UUID.randomUUID());

        deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setName("TestDeviceProfile");

        //node
        config = new EmptyNodeConfiguration();
        node = new TbDeviceTypeSwitchNode();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        //init mock
        ctx = mock(TbContext.class);
        deviceProfileCache = mock(RuleEngineDeviceProfileCache.class);
        callback = mock(TbMsgCallback.class);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getDeviceProfileCache()).thenReturn(deviceProfileCache);

        doReturn(deviceProfile).when(deviceProfileCache).get(tenantId, deviceId);
        doReturn(null).when(deviceProfileCache).get(tenantId, deviceIdDeleted);
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenMsg_whenOnMsg_then_Fail() {
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        assertThatThrownBy(() -> {
            node.onMsg(ctx, getTbMsg(customerId));
        }).isInstanceOf(TbNodeException.class).hasMessageContaining("Unsupported originator type");
    }

    @Test
    void givenMsg_whenOnMsg_EntityIdDeleted_then_Fail() {
        assertThatThrownBy(() -> {
            node.onMsg(ctx, getTbMsg(deviceIdDeleted));
        }).isInstanceOf(TbNodeException.class).hasMessageContaining("Device profile for entity id");
    }

    @Test
    void givenMsg_whenOnMsg_then_Success() throws TbNodeException {
        TbMsg msg = getTbMsg(deviceId);
        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq("TestDeviceProfile"));
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    private TbMsg getTbMsg(EntityId entityId) {
        return TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", entityId, new TbMsgMetaData(), "{}", callback);
    }
}
