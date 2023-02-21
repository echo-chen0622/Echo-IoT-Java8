package org.echoiot.server.common.msg;

import lombok.Data;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.msg.gen.MsgProtos;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.UUID;

@Data
public class TbMsgProcessingStackItem implements Serializable {

    @NotNull
    private final RuleChainId ruleChainId;
    @NotNull
    private final RuleNodeId ruleNodeId;

    @NotNull
    MsgProtos.TbMsgProcessingStackItemProto toProto() {
        return MsgProtos.TbMsgProcessingStackItemProto.newBuilder()
                .setRuleChainIdMSB(ruleChainId.getId().getMostSignificantBits())
                .setRuleChainIdLSB(ruleChainId.getId().getLeastSignificantBits())
                .setRuleNodeIdMSB(ruleNodeId.getId().getMostSignificantBits())
                .setRuleNodeIdLSB(ruleNodeId.getId().getLeastSignificantBits())
                .build();
    }

    @NotNull
    static TbMsgProcessingStackItem fromProto(@NotNull MsgProtos.TbMsgProcessingStackItemProto item){
        return new TbMsgProcessingStackItem(
                new RuleChainId(new UUID(item.getRuleChainIdMSB(), item.getRuleChainIdLSB())),
                new RuleNodeId(new UUID(item.getRuleNodeIdMSB(), item.getRuleNodeIdLSB()))
        );
    }

}
