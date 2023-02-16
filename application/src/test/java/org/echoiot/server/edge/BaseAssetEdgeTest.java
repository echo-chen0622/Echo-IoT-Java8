package org.echoiot.server.edge;

import com.google.protobuf.AbstractMessage;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.echoiot.server.gen.edge.v1.AssetUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract public class BaseAssetEdgeTest extends AbstractEdgeTest {

    @Test
    public void testAssets() throws Exception {
        // create asset and assign to edge
        edgeImitator.expectMessageAmount(2);
        Asset savedAsset = saveAsset("Edge Asset 2");
        doPost("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<AssetUpdateMsg> assetUpdateMsgOpt = edgeImitator.findMessageByType(AssetUpdateMsg.class);
        Assert.assertTrue(assetUpdateMsgOpt.isPresent());
        AssetUpdateMsg assetUpdateMsg = assetUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(savedAsset.getUuidId().getMostSignificantBits(), assetUpdateMsg.getIdMSB());
        Assert.assertEquals(savedAsset.getUuidId().getLeastSignificantBits(), assetUpdateMsg.getIdLSB());
        Assert.assertEquals(savedAsset.getName(), assetUpdateMsg.getName());
        Assert.assertEquals(savedAsset.getType(), assetUpdateMsg.getType());
        Optional<AssetProfileUpdateMsg> assetProfileUpdateMsgOpt = edgeImitator.findMessageByType(AssetProfileUpdateMsg.class);
        Assert.assertTrue(assetProfileUpdateMsgOpt.isPresent());
        AssetProfileUpdateMsg assetProfileUpdateMsg = assetProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        Assert.assertEquals(savedAsset.getAssetProfileId().getId().getMostSignificantBits(), assetProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(savedAsset.getAssetProfileId().getId().getLeastSignificantBits(), assetProfileUpdateMsg.getIdLSB());

        // update asset
        edgeImitator.expectMessageAmount(1);
        savedAsset.setName("Edge Asset 2 Updated");
        savedAsset = doPost("/api/asset", savedAsset, Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(savedAsset.getName(), assetUpdateMsg.getName());

        // unassign asset from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(savedAsset.getUuidId().getMostSignificantBits(), assetUpdateMsg.getIdMSB());
        Assert.assertEquals(savedAsset.getUuidId().getLeastSignificantBits(), assetUpdateMsg.getIdLSB());

        // delete asset - no messages expected
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/asset/" + savedAsset.getUuidId())
                .andExpect(status().isOk());
        Assert.assertFalse(edgeImitator.waitForMessages(1));

        // create asset #2 and assign to edge
        edgeImitator.expectMessageAmount(2);
        savedAsset = saveAsset("Edge Asset 3");
        doPost("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        assetUpdateMsgOpt = edgeImitator.findMessageByType(AssetUpdateMsg.class);
        Assert.assertTrue(assetUpdateMsgOpt.isPresent());
        assetUpdateMsg = assetUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(savedAsset.getUuidId().getMostSignificantBits(), assetUpdateMsg.getIdMSB());
        Assert.assertEquals(savedAsset.getUuidId().getLeastSignificantBits(), assetUpdateMsg.getIdLSB());
        Assert.assertEquals(savedAsset.getName(), assetUpdateMsg.getName());
        Assert.assertEquals(savedAsset.getType(), assetUpdateMsg.getType());
        assetProfileUpdateMsgOpt = edgeImitator.findMessageByType(AssetProfileUpdateMsg.class);
        Assert.assertTrue(assetProfileUpdateMsgOpt.isPresent());
        assetProfileUpdateMsg = assetProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        Assert.assertEquals(savedAsset.getAssetProfileId().getId().getMostSignificantBits(), assetProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(savedAsset.getAssetProfileId().getId().getLeastSignificantBits(), assetProfileUpdateMsg.getIdLSB());

        // assign asset #2 to customer
        Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        edgeImitator.expectMessageAmount(1);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomer.getUuidId().getMostSignificantBits(), assetUpdateMsg.getCustomerIdMSB());
        Assert.assertEquals(savedCustomer.getUuidId().getLeastSignificantBits(), assetUpdateMsg.getCustomerIdLSB());

        // unassign asset #2 from customer
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/customer/asset/" + savedAsset.getUuidId(), Asset.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(
                new CustomerId(EntityId.NULL_UUID),
                new CustomerId(new UUID(assetUpdateMsg.getCustomerIdMSB(), assetUpdateMsg.getCustomerIdLSB())));

        // delete asset #2 - messages expected
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/asset/" + savedAsset.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetUpdateMsg);
        assetUpdateMsg = (AssetUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        Assert.assertEquals(savedAsset.getUuidId().getMostSignificantBits(), assetUpdateMsg.getIdMSB());
        Assert.assertEquals(savedAsset.getUuidId().getLeastSignificantBits(), assetUpdateMsg.getIdLSB());
    }


}
