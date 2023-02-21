package org.echoiot.rule.engine.action;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Data
public class TbSaveToCustomCassandraTableNodeConfiguration implements NodeConfiguration<TbSaveToCustomCassandraTableNodeConfiguration> {


    private String tableName;
    private Map<String, String> fieldsMapping;


    @NotNull
    @Override
    public TbSaveToCustomCassandraTableNodeConfiguration defaultConfiguration() {
        @NotNull TbSaveToCustomCassandraTableNodeConfiguration configuration = new TbSaveToCustomCassandraTableNodeConfiguration();
        configuration.setTableName("");
        @NotNull Map<String, String> map = new HashMap<>();
        map.put("", "");
        configuration.setFieldsMapping(map);
        return configuration;
    }
}
