package org.echoiot.server.edge;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.ByteString;
import org.echoiot.server.common.data.asset.AssetProfile;
import org.echoiot.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract public class BaseAssetProfileEdgeTest extends AbstractEdgeTest {

    @Test
    public void testAssetProfiles() throws Exception {
        // create asset profile
        AssetProfile assetProfile = this.createAssetProfile("Building");
        edgeImitator.expectMessageAmount(1);
        assetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetProfileUpdateMsg);
        @NotNull AssetProfileUpdateMsg assetProfileUpdateMsg = (AssetProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        Assert.assertEquals(assetProfile.getUuidId().getMostSignificantBits(), assetProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(assetProfile.getUuidId().getLeastSignificantBits(), assetProfileUpdateMsg.getIdLSB());
        Assert.assertEquals("Building", assetProfileUpdateMsg.getName());

        // update asset profile
        assetProfile.setImage("IMAGE");
        edgeImitator.expectMessageAmount(1);
        assetProfile = doPost("/api/assetProfile", assetProfile, AssetProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetProfileUpdateMsg);
        assetProfileUpdateMsg = (AssetProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        Assert.assertEquals(ByteString.copyFrom("IMAGE".getBytes(StandardCharsets.UTF_8)), assetProfileUpdateMsg.getImage());

        // delete profile
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/assetProfile/" + assetProfile.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof AssetProfileUpdateMsg);
        assetProfileUpdateMsg = (AssetProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        Assert.assertEquals(assetProfile.getUuidId().getMostSignificantBits(), assetProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(assetProfile.getUuidId().getLeastSignificantBits(), assetProfileUpdateMsg.getIdLSB());
    }
}
