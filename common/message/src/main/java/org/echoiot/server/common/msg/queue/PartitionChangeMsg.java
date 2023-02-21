package org.echoiot.server.common.msg.queue;

import lombok.Data;
import lombok.Getter;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorMsg;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Andrew Shvayka
 */
@Data
public final class PartitionChangeMsg implements TbActorMsg {

    @NotNull
    @Getter
    private final ServiceType serviceType;
    @NotNull
    @Getter
    private final Set<TopicPartitionInfo> partitions;

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.PARTITION_CHANGE_MSG;
    }
}
