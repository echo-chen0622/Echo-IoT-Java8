package org.echoiot.server.common.msg;

import org.echoiot.server.common.data.FSTUtils;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TbMsgProcessingStackItemTest {

    @Test
    void testSerialization() {
        @NotNull TbMsgProcessingStackItem item = new TbMsgProcessingStackItem(new RuleChainId(UUID.randomUUID()), new RuleNodeId(UUID.randomUUID()));
        byte[] bytes = FSTUtils.encode(item);
        TbMsgProcessingStackItem itemDecoded = FSTUtils.decode(bytes);
        assertThat(item).isEqualTo(itemDecoded);
    }

}
