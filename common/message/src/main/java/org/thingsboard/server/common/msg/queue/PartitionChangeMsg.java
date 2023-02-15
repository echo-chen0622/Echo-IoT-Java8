package org.thingsboard.server.common.msg.queue;

import lombok.Data;
import lombok.Getter;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.Set;

/**
 * @author Andrew Shvayka
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
