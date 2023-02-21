package org.echoiot.rule.engine.geo;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Created by Echo on 19.01.18.
 */
@Data
public class TbGpsGeofencingActionNodeConfiguration extends TbGpsGeofencingFilterNodeConfiguration {

    private int minInsideDuration;
    private int minOutsideDuration;

    private String minInsideDurationTimeUnit;
    private String minOutsideDurationTimeUnit;

    @NotNull
    @Override
    public TbGpsGeofencingActionNodeConfiguration defaultConfiguration() {
        @NotNull TbGpsGeofencingActionNodeConfiguration configuration = new TbGpsGeofencingActionNodeConfiguration();
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
