package org.echoiot.rule.engine.filter;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import org.echoiot.rule.engine.api.ScriptEngine;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.data.script.ScriptLanguage;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgDataType;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TbJsSwitchNodeTest {

    private TbJsSwitchNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private ScriptEngine scriptEngine;

    private final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    @Test
    public void multipleRoutesAreAllowed() throws TbNodeException {
        initWithScript();
        @NotNull TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("temp", "10");
        metaData.putValue("humidity", "99");
        @NotNull String rawJson = "{\"name\": \"Vit\", \"passed\": 5}";

        @NotNull TbMsg msg = TbMsg.newMsg("USER", null, metaData, TbMsgDataType.JSON, rawJson, ruleChainId, ruleNodeId);
        when(scriptEngine.executeSwitchAsync(msg)).thenReturn(Futures.immediateFuture(Sets.newHashSet("one", "three")));

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, Sets.newHashSet("one", "three"));
    }

    private void initWithScript() throws TbNodeException {
        @NotNull TbJsSwitchNodeConfiguration config = new TbJsSwitchNodeConfiguration();
        config.setScriptLang(ScriptLanguage.JS);
        config.setJsScript("scr");
        @NotNull ObjectMapper mapper = new ObjectMapper();
        @NotNull TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

        when(ctx.createScriptEngine(ScriptLanguage.JS, "scr")).thenReturn(scriptEngine);

        node = new TbJsSwitchNode();
        node.init(ctx, nodeConfiguration);
    }
}
