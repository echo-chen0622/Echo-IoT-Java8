package org.echoiot.rule.engine.mail;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbEmail;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgDataType;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TbMsgToEmailNodeTest {

    private TbMsgToEmailNode emailNode;

    @Mock
    private TbContext ctx;

    private final EntityId originator = new DeviceId(Uuids.timeBased());
    private final TbMsgMetaData metaData = new TbMsgMetaData();
    private final String rawJson = "{\"name\": \"temp\", \"passed\": 5 , \"complex\": {\"val\":12, \"count\":100}}";

    private final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    @Test
    public void msgCanBeConverted() throws IOException {
        initWithScript();
        metaData.putValue("username", "oreo");
        metaData.putValue("userEmail", "user@email.io");
        metaData.putValue("name", "temp");
        metaData.putValue("passed", "5");
        metaData.putValue("count", "100");
        @NotNull TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);

        emailNode.onMsg(ctx, msg);

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        @NotNull ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        @NotNull ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        @NotNull ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());


        assertEquals("SEND_EMAIL", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertEquals("oreo", metadataCaptor.getValue().getValue("username"));
        assertNotSame(metaData, metadataCaptor.getValue());

        TbEmail actual = new ObjectMapper().readValue(dataCaptor.getValue().getBytes(), TbEmail.class);

        TbEmail expected = TbEmail.builder()
                .from("test@mail.org")
                .to("user@email.io")
                .subject("Hi oreo there")
                .body("temp is to high. Current 5 and 100")
                .build();
        assertEquals(expected, actual);
    }

    private void initWithScript() {
        try {
            @NotNull TbMsgToEmailNodeConfiguration config = new TbMsgToEmailNodeConfiguration();
            config.setFromTemplate("test@mail.org");
            config.setToTemplate("${userEmail}");
            config.setSubjectTemplate("Hi ${username} there");
            config.setBodyTemplate("${name} is to high. Current ${passed} and ${count}");
            config.setMailBodyType("false");
            @NotNull ObjectMapper mapper = new ObjectMapper();
            @NotNull TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

            emailNode = new TbMsgToEmailNode();
            emailNode.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
