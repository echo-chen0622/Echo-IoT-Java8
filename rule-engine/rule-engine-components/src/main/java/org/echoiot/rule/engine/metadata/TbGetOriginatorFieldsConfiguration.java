package org.echoiot.rule.engine.metadata;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

import java.util.HashMap;
import java.util.Map;

@Data
public class TbGetOriginatorFieldsConfiguration implements NodeConfiguration<TbGetOriginatorFieldsConfiguration> {

    private Map<String, String> fieldsMapping;
    private boolean ignoreNullStrings;

    @Override
    public TbGetOriginatorFieldsConfiguration defaultConfiguration() {
        TbGetOriginatorFieldsConfiguration configuration = new TbGetOriginatorFieldsConfiguration();
        Map<String, String> fieldsMapping = new HashMap<>();
        fieldsMapping.put("name", "originatorName");
        fieldsMapping.put("type", "originatorType");
        configuration.setFieldsMapping(fieldsMapping);
        configuration.setIgnoreNullStrings(false);
        return configuration;
    }
}
