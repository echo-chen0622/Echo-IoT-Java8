package org.echoiot.rule.engine.filter;

import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.*;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.common.msg.queue.TbMsgCallback;
import org.jetbrains.annotations.NotNull;
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

class TbAssetTypeSwitchNodeTest {

    TenantId tenantId;
    AssetId assetId;
    AssetId assetIdDeleted;
    AssetProfile assetProfile;
    TbContext ctx;
    TbAssetTypeSwitchNode node;
    EmptyNodeConfiguration config;
    TbMsgCallback callback;
    RuleEngineAssetProfileCache assetProfileCache;

    @BeforeEach
    void setUp() throws TbNodeException {
        tenantId = new TenantId(UUID.randomUUID());
        assetId = new AssetId(UUID.randomUUID());
        assetIdDeleted = new AssetId(UUID.randomUUID());

        assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        assetProfile.setName("TestAssetProfile");

        //node
        config = new EmptyNodeConfiguration();
        node = new TbAssetTypeSwitchNode();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        //init mock
        ctx = mock(TbContext.class);
        assetProfileCache = mock(RuleEngineAssetProfileCache.class);
        callback = mock(TbMsgCallback.class);

        when(ctx.getTenantId()).thenReturn(tenantId);
        when(ctx.getAssetProfileCache()).thenReturn(assetProfileCache);

        doReturn(assetProfile).when(assetProfileCache).get(tenantId, assetId);
        doReturn(null).when(assetProfileCache).get(tenantId, assetIdDeleted);
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenMsg_whenOnMsg_then_Fail() {
        @NotNull CustomerId customerId = new CustomerId(UUID.randomUUID());
        assertThatThrownBy(() -> {
            node.onMsg(ctx, getTbMsg(customerId));
        }).isInstanceOf(TbNodeException.class).hasMessageContaining("Unsupported originator type");
    }

    @Test
    void givenMsg_whenOnMsg_EntityIdDeleted_then_Fail() {
        assertThatThrownBy(() -> {
            node.onMsg(ctx, getTbMsg(assetIdDeleted));
        }).isInstanceOf(TbNodeException.class).hasMessageContaining("Asset profile for entity id");
    }

    @Test
    void givenMsg_whenOnMsg_then_Success() throws TbNodeException {
        @NotNull TbMsg msg = getTbMsg(assetId);
        node.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq("TestAssetProfile"));
        verify(ctx, never()).tellFailure(any(), any());

        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    @NotNull
    private TbMsg getTbMsg(EntityId entityId) {
        return TbMsg.newMsg("POST_ATTRIBUTES_REQUEST", entityId, new TbMsgMetaData(), "{}", callback);
    }
}
