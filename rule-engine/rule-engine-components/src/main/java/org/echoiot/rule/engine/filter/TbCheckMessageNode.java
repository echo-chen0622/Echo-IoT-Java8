package org.echoiot.rule.engine.filter;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.msg.TbMsg;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "check fields presence",
        relationTypes = {"True", "False"},
        configClazz = TbCheckMessageNodeConfiguration.class,
        nodeDescription = "Checks the presence of the specified fields in the message and/or metadata.",
        nodeDetails = "Checks the presence of the specified fields in the message and/or metadata. " +
                "By default, the rule node checks that all specified fields need to be present. " +
                "Uncheck the 'Check that all specified fields are present' if the presence of at least one field is sufficient.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeCheckMessageConfig")
public class TbCheckMessageNode implements TbNode {

    private static final Gson gson = new Gson();

    private TbCheckMessageNodeConfiguration config;
    private List<String> messageNamesList;
    private List<String> metadataNamesList;

    @Override
    public void init(TbContext tbContext, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbCheckMessageNodeConfiguration.class);
        messageNamesList = config.getMessageNames();
        metadataNamesList = config.getMetadataNames();
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) {
        try {
            if (config.isCheckAllKeys()) {
                ctx.tellNext(msg, allKeysData(msg) && allKeysMetadata(msg) ? "True" : "False");
            } else {
                ctx.tellNext(msg, atLeastOneData(msg) || atLeastOneMetadata(msg) ? "True" : "False");
            }
        } catch (Exception e) {
            ctx.tellFailure(msg, e);
        }
    }

    private boolean allKeysData(@NotNull TbMsg msg) {
        if (!messageNamesList.isEmpty()) {
            Map<String, String> dataMap = dataToMap(msg);
            return processAllKeys(messageNamesList, dataMap);
        }
        return true;
    }

    private boolean allKeysMetadata(@NotNull TbMsg msg) {
        if (!metadataNamesList.isEmpty()) {
            Map<String, String> metadataMap = metadataToMap(msg);
            return processAllKeys(metadataNamesList, metadataMap);
        }
        return true;
    }

    private boolean atLeastOneData(@NotNull TbMsg msg) {
        if (!messageNamesList.isEmpty()) {
            Map<String, String> dataMap = dataToMap(msg);
            return processAtLeastOne(messageNamesList, dataMap);
        }
        return false;
    }

    private boolean atLeastOneMetadata(@NotNull TbMsg msg) {
        if (!metadataNamesList.isEmpty()) {
            Map<String, String> metadataMap = metadataToMap(msg);
            return processAtLeastOne(metadataNamesList, metadataMap);
        }
        return false;
    }

    private boolean processAllKeys(@NotNull List<String> data, @NotNull Map<String, String> map) {
        for (String field : data) {
            if (!map.containsKey(field)) {
                return false;
            }
        }
        return true;
    }

    private boolean processAtLeastOne(@NotNull List<String> data, @NotNull Map<String, String> map) {
        for (String field : data) {
            if (map.containsKey(field)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> metadataToMap(@NotNull TbMsg msg) {
        return msg.getMetaData().getData();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> dataToMap(@NotNull TbMsg msg) {
        return (Map<String, String>) gson.fromJson(msg.getData(), Map.class);
    }

}
