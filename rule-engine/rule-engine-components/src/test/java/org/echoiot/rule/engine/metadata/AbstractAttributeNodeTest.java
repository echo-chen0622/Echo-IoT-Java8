package org.echoiot.rule.engine.metadata;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgDataType;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.dao.asset.AssetService;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.device.DeviceService;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.echoiot.server.dao.user.UserService;
import org.jetbrains.annotations.NotNull;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.echoiot.rule.engine.api.TbRelationTypes.FAILURE;
import static org.echoiot.server.common.data.DataConstants.SERVER_SCOPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public abstract class AbstractAttributeNodeTest {
    final CustomerId customerId = new CustomerId(Uuids.timeBased());
    final TenantId tenantId = TenantId.fromUUID(Uuids.timeBased());
    final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());
    final String keyAttrConf = "${word}";
    final String valueAttrConf = "${result}";
    @Mock
    TbContext ctx;
    @Mock
    AttributesService attributesService;
    @Mock
    TimeseriesService timeseriesService;
    @Mock
    UserService userService;
    @Mock
    AssetService assetService;
    @Mock
    DeviceService deviceService;
    TbMsg msg;
    Map<String, String> metaData;
    TbEntityGetAttrNode node;

    void init(TbEntityGetAttrNode node) throws TbNodeException {
        @NotNull ObjectMapper mapper = JacksonUtil.OBJECT_MAPPER;
        @NotNull TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(getTbNodeConfig()));

        metaData = new HashMap<>();
        metaData.putIfAbsent("word", "temperature");
        metaData.putIfAbsent("result", "answer");

        this.node = node;
        this.node.init(null, nodeConfiguration);
    }

    void errorThrownIfCannotLoadAttributes(@NotNull User user) {
        msg = TbMsg.newMsg("USER", user.getId(), new TbMsgMetaData(), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getAttributesService()).thenReturn(attributesService);
        when(attributesService.find(any(), eq(getEntityId()), eq(SERVER_SCOPE), anyCollection()))
                .thenThrow(new IllegalStateException("something wrong"));

        node.onMsg(ctx, msg);
        @NotNull final ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals("something wrong", value.getMessage());
        assertTrue(msg.getMetaData().getData().isEmpty());
    }

    void errorThrownIfCannotLoadAttributesAsync(@NotNull User user) {

        msg = TbMsg.newMsg("USER", user.getId(), new TbMsgMetaData(), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getAttributesService()).thenReturn(attributesService);
        when(attributesService.find(any(), eq(getEntityId()), eq(SERVER_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFailedFuture(new IllegalStateException("something wrong")));

        node.onMsg(ctx, msg);
        @NotNull final ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals("something wrong", value.getMessage());
        assertTrue(msg.getMetaData().getData().isEmpty());
    }

    void failedChainUsedIfCustomerCannotBeFound(@NotNull User user) {
        msg = TbMsg.newMsg("USER", user.getId(), new TbMsgMetaData(), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, FAILURE);
        assertTrue(msg.getMetaData().getData().isEmpty());
    }

    void entityAttributeAddedInMetadata(EntityId entityId, String type) {
        msg = TbMsg.newMsg(type, entityId, new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);
        entityAttributeFetched(getEntityId());
    }

    void usersCustomerAttributesFetched(@NotNull User user) {
        msg = TbMsg.newMsg("USER", user.getId(), new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        entityAttributeFetched(getEntityId());
    }

    void assetsCustomerAttributesFetched(@NotNull Asset asset) {
        msg = TbMsg.newMsg("ASSET", asset.getId(), new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        entityAttributeFetched(getEntityId());
    }

    void deviceCustomerAttributesFetched(@NotNull Device device) {
        msg = TbMsg.newMsg("DEVICE", device.getId(), new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        entityAttributeFetched(getEntityId());
    }

    void deviceCustomerTelemetryFetched(@NotNull Device device) throws TbNodeException {
        @NotNull ObjectMapper mapper = JacksonUtil.OBJECT_MAPPER;
        @NotNull TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(getTbNodeConfigForTelemetry()));

        TbEntityGetAttrNode node = getEmptyNode();
        node.init(null, nodeConfiguration);

        msg = TbMsg.newMsg("DEVICE", device.getId(), new TbMsgMetaData(metaData), TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        @NotNull List<TsKvEntry> timeseries = Lists.newArrayList(new BasicTsKvEntry(1L, new StringDataEntry("temperature", "highest")));

        when(ctx.getTimeseriesService()).thenReturn(timeseriesService);
        when(timeseriesService.findLatest(any(), eq(getEntityId()), anyCollection()))
                .thenReturn(Futures.immediateFuture(timeseries));

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        assertEquals(msg.getMetaData().getValue("answer"), "highest");
    }

    void entityAttributeFetched(EntityId entityId) {
        @NotNull List<AttributeKvEntry> attributes = Lists.newArrayList(new BaseAttributeKvEntry(new StringDataEntry("temperature", "high"), 1L));

        when(ctx.getAttributesService()).thenReturn(attributesService);
        when(attributesService.find(any(), eq(entityId), eq(SERVER_SCOPE), anyCollection()))
                .thenReturn(Futures.immediateFuture(attributes));

        node.onMsg(ctx, msg);
        verify(ctx).tellSuccess(msg);
        assertEquals(msg.getMetaData().getValue("answer"), "high");
    }

    TbGetEntityAttrNodeConfiguration getTbNodeConfig() {
        return getConfig(false);
    }

    TbGetEntityAttrNodeConfiguration getTbNodeConfigForTelemetry() {
        return getConfig(true);
    }

    @NotNull
    private TbGetEntityAttrNodeConfiguration getConfig(boolean isTelemetry) {
        @NotNull TbGetEntityAttrNodeConfiguration config = new TbGetEntityAttrNodeConfiguration();
        @NotNull Map<String, String> conf = new HashMap<>();
        conf.put(keyAttrConf, valueAttrConf);
        config.setAttrMapping(conf);
        config.setTelemetry(isTelemetry);
        return config;
    }

    protected abstract TbEntityGetAttrNode getEmptyNode();

    abstract EntityId getEntityId();

    void mockFindDevice(@NotNull Device device) {
        when(ctx.getDeviceService()).thenReturn(deviceService);
        when(deviceService.findDeviceByIdAsync(any(), eq(device.getId()))).thenReturn(Futures.immediateFuture(device));
    }

    void mockFindAsset(@NotNull Asset asset) {
        when(ctx.getAssetService()).thenReturn(assetService);
        when(assetService.findAssetByIdAsync(any(), eq(asset.getId()))).thenReturn(Futures.immediateFuture(asset));
    }

    void mockFindUser(@NotNull User user) {
        when(ctx.getUserService()).thenReturn(userService);
        when(userService.findUserByIdAsync(any(), eq(user.getId()))).thenReturn(Futures.immediateFuture(user));
    }
}
