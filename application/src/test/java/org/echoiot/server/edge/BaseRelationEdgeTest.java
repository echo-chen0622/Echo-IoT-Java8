package org.echoiot.server.edge;

import com.google.protobuf.AbstractMessage;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.gen.edge.v1.RelationRequestMsg;
import org.echoiot.server.gen.edge.v1.RelationUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.echoiot.server.gen.edge.v1.UplinkMsg;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract public class BaseRelationEdgeTest extends AbstractEdgeTest {


    @Test
    public void testRelations() throws Exception {
        // create relation
        @NotNull Device device = findDeviceByName("Edge Device 1");
        @NotNull Asset asset = findAssetByName("Edge Asset 1");
        @NotNull EntityRelation relation = new EntityRelation();
        relation.setType("test");
        relation.setFrom(device.getId());
        relation.setTo(asset.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        edgeImitator.expectMessageAmount(1);
        doPost("/api/relation", relation);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RelationUpdateMsg);
        @NotNull RelationUpdateMsg relationUpdateMsg = (RelationUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, relationUpdateMsg.getMsgType());
        Assert.assertEquals(relationUpdateMsg.getType(), relation.getType());
        Assert.assertEquals(relationUpdateMsg.getFromIdMSB(), relation.getFrom().getId().getMostSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getFromIdLSB(), relation.getFrom().getId().getLeastSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getToEntityType(), relation.getTo().getEntityType().name());
        Assert.assertEquals(relationUpdateMsg.getFromIdMSB(), relation.getFrom().getId().getMostSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getToIdLSB(), relation.getTo().getId().getLeastSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getToEntityType(), relation.getTo().getEntityType().name());
        Assert.assertEquals(relationUpdateMsg.getTypeGroup(), relation.getTypeGroup().name());

        // delete relation
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/relation?" +
                "fromId=" + relation.getFrom().getId().toString() +
                "&fromType=" + relation.getFrom().getEntityType().name() +
                "&relationType=" + relation.getType() +
                "&relationTypeGroup=" + relation.getTypeGroup().name() +
                "&toId=" + relation.getTo().getId().toString() +
                "&toType=" + relation.getTo().getEntityType().name())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RelationUpdateMsg);
        relationUpdateMsg = (RelationUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, relationUpdateMsg.getMsgType());
        Assert.assertEquals(relationUpdateMsg.getType(), relation.getType());
        Assert.assertEquals(relationUpdateMsg.getFromIdMSB(), relation.getFrom().getId().getMostSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getFromIdLSB(), relation.getFrom().getId().getLeastSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getToEntityType(), relation.getTo().getEntityType().name());
        Assert.assertEquals(relationUpdateMsg.getFromIdMSB(), relation.getFrom().getId().getMostSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getToIdLSB(), relation.getTo().getId().getLeastSignificantBits());
        Assert.assertEquals(relationUpdateMsg.getToEntityType(), relation.getTo().getEntityType().name());
        Assert.assertEquals(relationUpdateMsg.getTypeGroup(), relation.getTypeGroup().name());
    }

    @Test
    public void testSendRelationToCloud() throws Exception {
        @NotNull Device device1 = saveDeviceOnCloudAndVerifyDeliveryToEdge();
        @NotNull Device device2 = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        RelationUpdateMsg.Builder relationUpdateMsgBuilder = RelationUpdateMsg.newBuilder();
        relationUpdateMsgBuilder.setType("test");
        relationUpdateMsgBuilder.setTypeGroup(RelationTypeGroup.COMMON.name());
        relationUpdateMsgBuilder.setToIdMSB(device1.getId().getId().getMostSignificantBits());
        relationUpdateMsgBuilder.setToIdLSB(device1.getId().getId().getLeastSignificantBits());
        relationUpdateMsgBuilder.setToEntityType(device1.getId().getEntityType().name());
        relationUpdateMsgBuilder.setFromIdMSB(device2.getId().getId().getMostSignificantBits());
        relationUpdateMsgBuilder.setFromIdLSB(device2.getId().getId().getLeastSignificantBits());
        relationUpdateMsgBuilder.setFromEntityType(device2.getId().getEntityType().name());
        relationUpdateMsgBuilder.setAdditionalInfo("{}");
        testAutoGeneratedCodeByProtobuf(relationUpdateMsgBuilder);
        uplinkMsgBuilder.addRelationUpdateMsg(relationUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());

        EntityRelation relation = doGet("/api/relation?" +
                "&fromId=" + device2.getUuidId() +
                "&fromType=" + device2.getId().getEntityType().name() +
                "&relationType=" + "test" +
                "&relationTypeGroup=" + RelationTypeGroup.COMMON.name() +
                "&toId=" + device1.getUuidId() +
                "&toType=" + device1.getId().getEntityType().name(), EntityRelation.class);
        Assert.assertNotNull(relation);
    }

    @Test
    public void testSendRelationRequestToCloud() throws Exception {
        @NotNull Device device = findDeviceByName("Edge Device 1");
        @NotNull Asset asset = findAssetByName("Edge Asset 1");

        @NotNull EntityRelation relation = new EntityRelation();
        relation.setType("test");
        relation.setFrom(device.getId());
        relation.setTo(asset.getId());
        relation.setTypeGroup(RelationTypeGroup.COMMON);

        edgeImitator.expectMessageAmount(1);
        doPost("/api/relation", relation);
        Assert.assertTrue(edgeImitator.waitForMessages());

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        RelationRequestMsg.Builder relationRequestMsgBuilder = RelationRequestMsg.newBuilder();
        relationRequestMsgBuilder.setEntityIdMSB(device.getId().getId().getMostSignificantBits());
        relationRequestMsgBuilder.setEntityIdLSB(device.getId().getId().getLeastSignificantBits());
        relationRequestMsgBuilder.setEntityType(device.getId().getEntityType().name());
        testAutoGeneratedCodeByProtobuf(relationRequestMsgBuilder);

        uplinkMsgBuilder.addRelationRequestMsg(relationRequestMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof RelationUpdateMsg);
        @NotNull RelationUpdateMsg relationUpdateMsg = (RelationUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, relationUpdateMsg.getMsgType());
        Assert.assertEquals(relation.getType(), relationUpdateMsg.getType());

        @NotNull UUID fromUUID = new UUID(relationUpdateMsg.getFromIdMSB(), relationUpdateMsg.getFromIdLSB());
        EntityId fromEntityId = EntityIdFactory.getByTypeAndUuid(relationUpdateMsg.getFromEntityType(), fromUUID);
        Assert.assertEquals(relation.getFrom(), fromEntityId);

        @NotNull UUID toUUID = new UUID(relationUpdateMsg.getToIdMSB(), relationUpdateMsg.getToIdLSB());
        EntityId toEntityId = EntityIdFactory.getByTypeAndUuid(relationUpdateMsg.getToEntityType(), toUUID);
        Assert.assertEquals(relation.getTo(), toEntityId);

        Assert.assertEquals(relation.getTypeGroup().name(), relationUpdateMsg.getTypeGroup());
    }
}
