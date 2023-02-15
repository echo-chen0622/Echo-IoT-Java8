package org.thingsboard.rule.engine.geo;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ashvayka on 19.01.18.
 */
@Data
public class TbGpsGeofencingActionNodeConfiguration extends TbGpsGeofencingFilterNodeConfiguration {

    private int minInsideDuration;
    private int minOutsideDuration;

    private String minInsideDurationTimeUnit;
    private String minOutsideDurationTimeUnit;

    @Override
    public TbGpsGeofencingActionNodeConfiguration defaultConfiguration() {
        TbGpsGeofencingActionNodeConfiguration configuration = new TbGpsGeofencingActionNodeConfiguration();
        configuration.setLatitudeKeyName("latitude");
        configuration.setLongitudeKeyName("longitude");
        configuration.setPerimeterType(PerimeterType.POLYGON);
        configuration.setFetchPerimeterInfoFromMessageMetadata(true);
        configuration.setPerimeterKeyName("ss_perimeter");
        configuration.setMinInsideDurationTimeUnit(TimeUnit.MINUTES.name());
        configuration.setMinOutsideDurationTimeUnit(TimeUnit.MINUTES.name());
        configuration.setMinInsideDuration(1);
        configuration.setMinOutsideDuration(1);
        return configuration;
    }
}
