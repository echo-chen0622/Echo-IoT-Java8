package org.echoiot.server.edge;

import com.google.protobuf.AbstractMessage;
import org.echoiot.server.common.data.queue.*;
import org.echoiot.server.common.msg.queue.ServiceType;
import org.echoiot.server.gen.edge.v1.QueueUpdateMsg;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;
import org.junit.Assert;
import org.junit.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract public class BaseQueueEdgeTest extends AbstractEdgeTest {

    @Test
    public void testQueues() throws Exception {
        loginSysAdmin();

        // create queue
        Queue queue = new Queue();
        queue.setName("EdgeMain");
        queue.setTopic("tb_rule_engine.EdgeMain");
        queue.setPollInterval(25);
        queue.setPartitions(10);
        queue.setConsumerPerPartition(false);
        queue.setPackProcessingTimeout(2000);
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        queue.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.RETRY_ALL);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0.7);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(5);
        queue.setProcessingStrategy(processingStrategy);

        edgeImitator.expectMessageAmount(1);
        Queue savedQueue = doPost("/api/queues?serviceType=" + ServiceType.TB_RULE_ENGINE.name(), queue, Queue.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof QueueUpdateMsg);
        QueueUpdateMsg queueUpdateMsg = (QueueUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, queueUpdateMsg.getMsgType());
        Assert.assertEquals(savedQueue.getUuidId().getMostSignificantBits(), queueUpdateMsg.getIdMSB());
        Assert.assertEquals(savedQueue.getUuidId().getLeastSignificantBits(), queueUpdateMsg.getIdLSB());
        Assert.assertEquals(savedQueue.getTenantId().getId().getMostSignificantBits(), queueUpdateMsg.getTenantIdMSB());
        Assert.assertEquals(savedQueue.getTenantId().getId().getLeastSignificantBits(), queueUpdateMsg.getTenantIdLSB());
        Assert.assertEquals("EdgeMain", queueUpdateMsg.getName());
        Assert.assertEquals("tb_rule_engine.EdgeMain", queueUpdateMsg.getTopic());
        Assert.assertEquals(25, queueUpdateMsg.getPollInterval());
        Assert.assertEquals(10, queueUpdateMsg.getPartitions());
        Assert.assertFalse(queueUpdateMsg.getConsumerPerPartition());
        Assert.assertEquals(2000, queueUpdateMsg.getPackProcessingTimeout());
        Assert.assertEquals(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR.name(), queueUpdateMsg.getSubmitStrategy().getType());
        Assert.assertEquals(0, queueUpdateMsg.getSubmitStrategy().getBatchSize());
        Assert.assertEquals(ProcessingStrategyType.RETRY_ALL.name(), queueUpdateMsg.getProcessingStrategy().getType());
        Assert.assertEquals(3, queueUpdateMsg.getProcessingStrategy().getRetries());
        Assert.assertEquals(0.7, queueUpdateMsg.getProcessingStrategy().getFailurePercentage(), 1);
        Assert.assertEquals(3, queueUpdateMsg.getProcessingStrategy().getPauseBetweenRetries());
        Assert.assertEquals(5, queueUpdateMsg.getProcessingStrategy().getMaxPauseBetweenRetries());

        // update queue
        edgeImitator.expectMessageAmount(1);
        savedQueue.setPollInterval(50);
        savedQueue = doPost("/api/queues?serviceType=" + ServiceType.TB_RULE_ENGINE.name(), savedQueue, Queue.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof QueueUpdateMsg);
        queueUpdateMsg = (QueueUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, queueUpdateMsg.getMsgType());
        Assert.assertEquals(50, queueUpdateMsg.getPollInterval());

        // delete queue
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/queues/" + savedQueue.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof QueueUpdateMsg);
        queueUpdateMsg = (QueueUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, queueUpdateMsg.getMsgType());
        Assert.assertEquals(savedQueue.getUuidId().getMostSignificantBits(), queueUpdateMsg.getIdMSB());
        Assert.assertEquals(savedQueue.getUuidId().getLeastSignificantBits(), queueUpdateMsg.getIdLSB());
    }

}