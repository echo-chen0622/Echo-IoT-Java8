package org.echoiot.rule.engine.action;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class TbLogNodeTest {

    @Test
    void givenMsg_whenToLog_thenReturnString() {
        @NotNull TbLogNode node = new TbLogNode();
        @NotNull String data = "{\"key\": \"value\"}";
        @NotNull TbMsgMetaData metaData = new TbMsgMetaData(Map.of("mdKey1", "mdValue1", "mdKey2", "23"));
        @NotNull TbMsg msg = TbMsg.newMsg("POST_TELEMETRY", TenantId.SYS_TENANT_ID, metaData, data);

        @NotNull String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "{\"key\": \"value\"}\n" +
                "Incoming metadata:\n" +
                "{\"mdKey1\":\"mdValue1\",\"mdKey2\":\"23\"}");
    }

    @Test
    void givenEmptyDataMsg_whenToLog_thenReturnString() {
        @NotNull TbLogNode node = new TbLogNode();
        @NotNull TbMsgMetaData metaData = new TbMsgMetaData(Collections.emptyMap());
        @NotNull TbMsg msg = TbMsg.newMsg("POST_TELEMETRY", TenantId.SYS_TENANT_ID, metaData, "");

        @NotNull String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "\n" +
                "Incoming metadata:\n" +
                "{}");
    }
    @Test
    void givenNullDataMsg_whenToLog_thenReturnString() {
        @NotNull TbLogNode node = new TbLogNode();
        @NotNull TbMsgMetaData metaData = new TbMsgMetaData(Collections.emptyMap());
        @NotNull TbMsg msg = TbMsg.newMsg("POST_TELEMETRY", TenantId.SYS_TENANT_ID, metaData, null);

        @NotNull String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "null\n" +
                "Incoming metadata:\n" +
                "{}");
    }

}
