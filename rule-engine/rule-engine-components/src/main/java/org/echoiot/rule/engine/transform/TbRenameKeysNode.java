package org.echoiot.rule.engine.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.rule.engine.api.*;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "rename keys",
        configClazz = TbRenameKeysNodeConfiguration.class,
        nodeDescription = "Renames msg data or metadata keys to the new key names selected in the key mapping.",
        nodeDetails = "If the key that is selected in the key mapping is missed in the selected msg source(data or metadata), it will be ignored." +
                " Returns transformed messages via <code>Success</code> chain",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeRenameKeysConfig",
        icon = "find_replace"
)
public class TbRenameKeysNode implements TbNode {

    private TbRenameKeysNodeConfiguration config;
    private Map<String, String> renameKeysMapping;
    private boolean fromMetadata;

    @Override
    public void init(TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbRenameKeysNodeConfiguration.class);
        this.renameKeysMapping = config.getRenameKeysMapping();
        this.fromMetadata = config.isFromMetadata();
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        TbMsgMetaData metaData = msg.getMetaData();
        @Nullable String data = msg.getData();
        boolean msgChanged = false;
        if (fromMetadata) {
            Map<String, String> metaDataMap = metaData.getData();
            for (@NotNull Map.Entry<String, String> entry : renameKeysMapping.entrySet()) {
                String nameKey = entry.getKey();
                if (metaDataMap.containsKey(nameKey)) {
                    msgChanged = true;
                    metaDataMap.put(entry.getValue(), metaDataMap.get(nameKey));
                    metaDataMap.remove(nameKey);
                }
            }
            metaData = new TbMsgMetaData(metaDataMap);
        } else {
            JsonNode dataNode = JacksonUtil.toJsonNode(msg.getData());
            if (dataNode.isObject()) {
                @NotNull ObjectNode msgData = (ObjectNode) dataNode;
                for (@NotNull Map.Entry<String, String> entry : renameKeysMapping.entrySet()) {
                    String nameKey = entry.getKey();
                    if (msgData.has(nameKey)) {
                        msgChanged = true;
                        msgData.set(entry.getValue(), msgData.get(nameKey));
                        msgData.remove(nameKey);
                    }
                }
                data = JacksonUtil.toString(msgData);
            }
        }
        if (msgChanged) {
            ctx.tellSuccess(TbMsg.transformMsg(msg, msg.getType(), msg.getOriginator(), metaData, data));
        } else {
            ctx.tellSuccess(msg);
        }
    }
}
