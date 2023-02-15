package org.thingsboard.rule.engine.metadata;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TbGetCustomerAttributeNodeTest extends AbstractAttributeNodeTest {
    User user = new User();
    Asset asset = new Asset();
    Device device = new Device();

    @Before
    public void initDataForTests() throws TbNodeException {
        init(new TbGetCustomerAttributeNode());
        user.setCustomerId(customerId);
        user.setId(new UserId(UUID.randomUUID()));

        asset.setCustomerId(customerId);
        asset.setId(new AssetId(UUID.randomUUID()));

        device.setCustomerId(customerId);
        device.setId(new DeviceId(Uuids.timeBased()));
    }

    @Override
    protected TbEntityGetAttrNode getEmptyNode() {
        return new TbGetCustomerAttributeNode();
    }

    @Override
    EntityId getEntityId() {
        return customerId;
    }

    @Test
    public void errorThrownIfCannotLoadAttributes() {
        mockFindUser(user);
        errorThrownIfCannotLoadAttributes(user);
    }

    @Test
    public void errorThrownIfCannotLoadAttributesAsync() {
        mockFindUser(user);
        errorThrownIfCannotLoadAttributesAsync(user);
    }

    @Test
    public void failedChainUsedIfCustomerCannotBeFound() {
        when(ctx.getUserService()).thenReturn(userService);
        when(userService.findUserByIdAsync(any(), eq(user.getId()))).thenReturn(Futures.immediateFuture(null));
        failedChainUsedIfCustomerCannotBeFound(user);
    }

    @Test
    public void customerAttributeAddedInMetadata() {
        entityAttributeAddedInMetadata(customerId, "CUSTOMER");
    }

    @Test
    public void usersCustomerAttributesFetched() {
        mockFindUser(user);
        usersCustomerAttributesFetched(user);
    }

    @Test
    public void assetsCustomerAttributesFetched() {
        mockFindAsset(asset);
        assetsCustomerAttributesFetched(asset);
    }

    @Test
    public void deviceCustomerAttributesFetched() {
        mockFindDevice(device);
        deviceCustomerAttributesFetched(device);
    }

    @Test
    public void deviceCustomerTelemetryFetched() throws TbNodeException {
        mockFindDevice(device);
        deviceCustomerTelemetryFetched(device);
    }
}
