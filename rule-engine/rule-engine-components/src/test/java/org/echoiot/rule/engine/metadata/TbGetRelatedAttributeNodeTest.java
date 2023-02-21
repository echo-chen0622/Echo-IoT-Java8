package org.echoiot.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.UserId;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.dao.relation.RelationService;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TbGetRelatedAttributeNodeTest extends AbstractAttributeNodeTest {
    @NotNull
    User user = new User();
    @NotNull
    Asset asset = new Asset();
    @NotNull
    Device device = new Device();
    @Mock
    private RelationService relationService;
    private EntityRelation entityRelation;

    @Before
    public void initDataForTests() throws TbNodeException {
        init(new TbGetRelatedAttributeNode());
        entityRelation = new EntityRelation();
        entityRelation.setTo(customerId);
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);
        when(ctx.getRelationService()).thenReturn(relationService);

        user.setCustomerId(customerId);
        user.setId(new UserId(UUID.randomUUID()));
        entityRelation.setFrom(user.getId());

        asset.setCustomerId(customerId);
        asset.setId(new AssetId(UUID.randomUUID()));

        device.setCustomerId(customerId);
        device.setId(new DeviceId(UUID.randomUUID()));
    }

    @NotNull
    @Override
    protected TbEntityGetAttrNode getEmptyNode() {
        return new TbGetRelatedAttributeNode();
    }

    @NotNull
    @Override
    TbGetEntityAttrNodeConfiguration getTbNodeConfig() {
        return getConfig(false);
    }

    @NotNull
    @Override
    TbGetEntityAttrNodeConfiguration getTbNodeConfigForTelemetry() {
        return getConfig(true);
    }

    @NotNull
    private TbGetEntityAttrNodeConfiguration getConfig(boolean isTelemetry) {
        @NotNull TbGetRelatedAttrNodeConfiguration config = new TbGetRelatedAttrNodeConfiguration();
        config = config.defaultConfiguration();
        @NotNull Map<String, String> conf = new HashMap<>();
        conf.put(keyAttrConf, valueAttrConf);
        config.setAttrMapping(conf);
        config.setTelemetry(isTelemetry);
        return config;
    }

    @NotNull
    @Override
    EntityId getEntityId() {
        return customerId;
    }

    @Test
    public void errorThrownIfCannotLoadAttributes() {
        entityRelation.setFrom(user.getId());
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        errorThrownIfCannotLoadAttributes(user);
    }

    @Test
    public void errorThrownIfCannotLoadAttributesAsync() {
        entityRelation.setFrom(user.getId());
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        errorThrownIfCannotLoadAttributesAsync(user);
    }

    @Test
    public void failedChainUsedIfCustomerCannotBeFound() {
        entityRelation.setFrom(customerId);
        entityRelation.setTo(null);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        failedChainUsedIfCustomerCannotBeFound(user);
    }

    @Test
    public void customerAttributeAddedInMetadata() {
        entityRelation.setFrom(customerId);
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        entityAttributeAddedInMetadata(customerId, "CUSTOMER");
    }

    @Test
    public void usersCustomerAttributesFetched() {
        entityRelation.setFrom(user.getId());
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        usersCustomerAttributesFetched(user);
    }

    @Test
    public void assetsCustomerAttributesFetched() {
        entityRelation.setFrom(asset.getId());
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        assetsCustomerAttributesFetched(asset);
    }

    @Test
    public void deviceCustomerAttributesFetched() {
        entityRelation.setFrom(device.getId());
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        deviceCustomerAttributesFetched(device);
    }

    @Test
    public void deviceCustomerTelemetryFetched() throws TbNodeException {
        entityRelation.setFrom(device.getId());
        entityRelation.setTo(customerId);
        when(relationService.findByQuery(any(), any())).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        deviceCustomerTelemetryFetched(device);
    }
}
