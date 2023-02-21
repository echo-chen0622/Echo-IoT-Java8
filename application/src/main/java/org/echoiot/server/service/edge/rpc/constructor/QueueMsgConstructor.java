package org.echoiot.server.service.edge.rpc.constructor;

import org.echoiot.server.common.data.id.QueueId;
import org.echoiot.server.common.data.queue.ProcessingStrategy;
import org.echoiot.server.common.data.queue.Queue;
import org.echoiot.server.common.data.queue.SubmitStrategy;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.echoiot.server.gen.edge.v1.ProcessingStrategyProto;
import org.echoiot.server.gen.edge.v1.QueueUpdateMsg;
import org.echoiot.server.gen.edge.v1.SubmitStrategyProto;
import org.echoiot.server.gen.edge.v1.UpdateMsgType;

@Component
@TbCoreComponent
public class QueueMsgConstructor {

    @NotNull
    public QueueUpdateMsg constructQueueUpdatedMsg(UpdateMsgType msgType, @NotNull Queue queue) {
        @NotNull QueueUpdateMsg.Builder builder = QueueUpdateMsg.newBuilder()
                                                                .setMsgType(msgType)
                                                                .setIdMSB(queue.getId().getId().getMostSignificantBits())
                                                                .setIdLSB(queue.getId().getId().getLeastSignificantBits())
                                                                .setTenantIdMSB(queue.getTenantId().getId().getMostSignificantBits())
                                                                .setTenantIdLSB(queue.getTenantId().getId().getLeastSignificantBits())
                                                                .setName(queue.getName())
                                                                .setTopic(queue.getTopic())
                                                                .setPollInterval(queue.getPollInterval())
                                                                .setPartitions(queue.getPartitions())
                                                                .setConsumerPerPartition(queue.isConsumerPerPartition())
                                                                .setPackProcessingTimeout(queue.getPackProcessingTimeout())
                                                                .setSubmitStrategy(createSubmitStrategyProto(queue.getSubmitStrategy()))
                                                                .setProcessingStrategy(createProcessingStrategyProto(queue.getProcessingStrategy()));
        return builder.build();
    }

    @NotNull
    private ProcessingStrategyProto createProcessingStrategyProto(@NotNull ProcessingStrategy processingStrategy) {
        return ProcessingStrategyProto.newBuilder()
                .setType(processingStrategy.getType().name())
                .setRetries(processingStrategy.getRetries())
                .setFailurePercentage(processingStrategy.getFailurePercentage())
                .setPauseBetweenRetries(processingStrategy.getPauseBetweenRetries())
                .setMaxPauseBetweenRetries(processingStrategy.getMaxPauseBetweenRetries())
                .build();
    }

    @NotNull
    private SubmitStrategyProto createSubmitStrategyProto(@NotNull SubmitStrategy submitStrategy) {
        return SubmitStrategyProto.newBuilder()
                .setType(submitStrategy.getType().name())
                .setBatchSize(submitStrategy.getBatchSize())
                .build();
    }

    @NotNull
    public QueueUpdateMsg constructQueueDeleteMsg(@NotNull QueueId queueId) {
        return QueueUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(queueId.getId().getMostSignificantBits())
                .setIdLSB(queueId.getId().getLeastSignificantBits()).build();
    }

}
