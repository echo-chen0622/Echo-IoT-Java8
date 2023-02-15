package org.thingsboard.server.common.msg;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.FSTUtils;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TbMsgProcessingStackItemTest {

    @Test
    void testSerialization() {
        TbMsgProcessingStackItem item = new TbMsgProcessingStackItem(new RuleChainId(UUID.randomUUID()), new RuleNodeId(UUID.randomUUID()));
        byte[] bytes = FSTUtils.encode(item);
        TbMsgProcessingStackItem itemDecoded = FSTUtils.decode(bytes);
        assertThat(item).isEqualTo(itemDecoded);
    }

}
