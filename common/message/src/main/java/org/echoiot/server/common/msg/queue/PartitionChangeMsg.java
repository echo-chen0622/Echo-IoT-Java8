package org.echoiot.server.common.msg.queue;

import lombok.Data;
import lombok.Getter;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.TbActorMsg;

import java.util.Set;

/**
 * @author Echo
 */
@Data
public final class PartitionChangeMsg implements TbActorMsg {

    @Getter
    private final ServiceType serviceType;
    @Getter
    private final Set<TopicPartitionInfo> partitions;

    @Override
    public MsgType getMsgType() {
        return MsgType.PARTITION_CHANGE_MSG;
    }
}
