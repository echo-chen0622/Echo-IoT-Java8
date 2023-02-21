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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "delete keys",
        configClazz = TbDeleteKeysNodeConfiguration.class,
        nodeDescription = "Removes keys from the msg data or metadata with the specified key names selected in the list",
        nodeDetails = "Will fetch fields (regex) values specified in list. If specified field (regex) is not part of msg " +
                "or metadata fields it will be ignored. Returns transformed messages via <code>Success</code> chain",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeDeleteKeysConfig",
        icon = "remove_circle"
)
public class TbDeleteKeysNode implements TbNode {

    private TbDeleteKeysNodeConfiguration config;
    private List<Pattern> patternKeys;
    private boolean fromMetadata;

    @Override
    public void init(TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbDeleteKeysNodeConfiguration.class);
        this.fromMetadata = config.isFromMetadata();
        this.patternKeys = new ArrayList<>();
        config.getKeys().forEach(key -> {
            this.patternKeys.add(Pattern.compile(key));
        });
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        TbMsgMetaData metaData = msg.getMetaData();
        @Nullable String msgData = msg.getData();
        @NotNull List<String> keysToDelete = new ArrayList<>();
        if (fromMetadata) {
            Map<String, String> metaDataMap = metaData.getData();
            metaDataMap.forEach((keyMetaData, valueMetaData) -> {
                if (checkKey(keyMetaData)) {
                    keysToDelete.add(keyMetaData);
                }
            });
            keysToDelete.forEach(key -> metaDataMap.remove(key));
            metaData = new TbMsgMetaData(metaDataMap);
        } else {
            JsonNode dataNode = JacksonUtil.toJsonNode(msgData);
            if (dataNode.isObject()) {
                @NotNull ObjectNode msgDataObject = (ObjectNode) dataNode;
                dataNode.fields().forEachRemaining(entry -> {
                    String keyData = entry.getKey();
                    if (checkKey(keyData)) {
                        keysToDelete.add(keyData);
                    }
                });
                msgDataObject.remove(keysToDelete);
                msgData = JacksonUtil.toString(msgDataObject);
            }
        }
        if (keysToDelete.isEmpty()) {
            ctx.tellSuccess(msg);
        } else {
            ctx.tellSuccess(TbMsg.transformMsg(msg, msg.getType(), msg.getOriginator(), metaData, msgData));
        }
    }

    boolean checkKey(@NotNull String key) {
        return patternKeys.stream().anyMatch(pattern -> pattern.matcher(key).matches());
    }
}
