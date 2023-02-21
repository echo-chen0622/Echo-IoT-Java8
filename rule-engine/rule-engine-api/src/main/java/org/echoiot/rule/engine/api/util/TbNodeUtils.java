package org.echoiot.rule.engine.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 19.01.18.
 */
public class TbNodeUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Pattern DATA_PATTERN = Pattern.compile("(\\$\\[)(.*?)(])");

    public static <T> T convert(@NotNull TbNodeConfiguration configuration, Class<T> clazz) throws TbNodeException {
        try {
            return mapper.treeToValue(configuration.getData(), clazz);
        } catch (JsonProcessingException e) {
            throw new TbNodeException(e);
        }
    }

    @NotNull
    public static List<String> processPatterns(@NotNull List<String> patterns, @NotNull TbMsg tbMsg) {
        if (!CollectionUtils.isEmpty(patterns)) {
            return patterns.stream().map(p -> processPattern(p, tbMsg)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public static String processPattern(String pattern, @NotNull TbMsg tbMsg) {
        try {
            String result = processPattern(pattern, tbMsg.getMetaData());
            JsonNode json = mapper.readTree(tbMsg.getData());
            if (json.isObject()) {
                @NotNull Matcher matcher = DATA_PATTERN.matcher(result);
                while (matcher.find()) {
                    String group = matcher.group(2);
                    @NotNull String[] keys = group.split("\\.");
                    @Nullable JsonNode jsonNode = json;
                    for (String key : keys) {
                        if (!StringUtils.isEmpty(key) && jsonNode != null) {
                            jsonNode = jsonNode.get(key);
                        } else {
                            jsonNode = null;
                            break;
                        }
                    }

                    if (jsonNode != null && jsonNode.isValueNode()) {
                        result = result.replace(formatDataVarTemplate(group), jsonNode.asText());
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to process pattern!", e);
        }
    }

    @NotNull
    public static List<String> processPatterns(@NotNull List<String> patterns, @NotNull TbMsgMetaData metaData) {
        if (!CollectionUtils.isEmpty(patterns)) {
            return patterns.stream().map(p -> processPattern(p, metaData)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public static String processPattern(String pattern, @NotNull TbMsgMetaData metaData) {
        return processTemplate(pattern, metaData.values());
    }

    public static String processTemplate(String template, @NotNull Map<String, String> data) {
        String result = template;
        for (@NotNull Map.Entry<String, String> kv : data.entrySet()) {
            result = processVar(result, kv.getKey(), kv.getValue());
        }
        return result;
    }

    @NotNull
    private static String processVar(@NotNull String pattern, String key, @NotNull String val) {
        return pattern.replace(formatMetadataVarTemplate(key), val);
    }

    @NotNull
    static String formatDataVarTemplate(String key) {
        return "$[" + key + ']';
    }

    @NotNull
    static String formatMetadataVarTemplate(String key) {
        return "${" + key + '}';
    }
}
