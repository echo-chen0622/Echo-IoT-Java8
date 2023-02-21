package org.echoiot.rule.engine.metadata;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.server.common.data.kv.Aggregation;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by mshvayka on 04.09.18.
 */
@Data
public class TbGetTelemetryNodeConfiguration implements NodeConfiguration<TbGetTelemetryNodeConfiguration> {

    public static final String FETCH_MODE_FIRST = "FIRST";
    public static final String FETCH_MODE_LAST = "LAST";
    public static final String FETCH_MODE_ALL = "ALL";

    public static final int MAX_FETCH_SIZE = 1000;

    private int startInterval;
    private int endInterval;

    private String startIntervalPattern;
    private String endIntervalPattern;

    private boolean useMetadataIntervalPatterns;

    private String startIntervalTimeUnit;
    private String endIntervalTimeUnit;
    private String fetchMode; //FIRST, LAST, ALL
    private String orderBy; //ASC, DESC
    private String aggregation; //MIN, MAX, AVG, SUM, COUNT, NONE;
    private int limit;

    private List<String> latestTsKeyNames;

    @NotNull
    @Override
    public TbGetTelemetryNodeConfiguration defaultConfiguration() {
        @NotNull TbGetTelemetryNodeConfiguration configuration = new TbGetTelemetryNodeConfiguration();
        configuration.setLatestTsKeyNames(Collections.emptyList());
        configuration.setFetchMode("FIRST");
        configuration.setStartIntervalTimeUnit(TimeUnit.MINUTES.name());
        configuration.setStartInterval(2);
        configuration.setEndIntervalTimeUnit(TimeUnit.MINUTES.name());
        configuration.setEndInterval(1);
        configuration.setUseMetadataIntervalPatterns(false);
        configuration.setStartIntervalPattern("");
        configuration.setEndIntervalPattern("");
        configuration.setOrderBy("ASC");
        configuration.setAggregation(Aggregation.NONE.name());
        configuration.setLimit(MAX_FETCH_SIZE);
        return configuration;
    }
}
