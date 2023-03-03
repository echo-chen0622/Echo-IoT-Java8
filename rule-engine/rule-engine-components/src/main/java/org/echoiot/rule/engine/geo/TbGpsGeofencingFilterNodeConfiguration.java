package org.echoiot.rule.engine.geo;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;

/**
 * Created by Echo on 19.01.18.
 */
@Data
public class TbGpsGeofencingFilterNodeConfiguration implements NodeConfiguration {

    private String latitudeKeyName;
    private String longitudeKeyName;
    private PerimeterType perimeterType;

    private boolean fetchPerimeterInfoFromMessageMetadata;
    // If Perimeter is fetched from metadata
    private String perimeterKeyName;

    //For Polygons
    private String polygonsDefinition;

    //For Circles
    private Double centerLatitude;
    private Double centerLongitude;
    private Double range;
    private RangeUnit rangeUnit;

    @Override
    public TbGpsGeofencingFilterNodeConfiguration defaultConfiguration() {
        TbGpsGeofencingFilterNodeConfiguration configuration = new TbGpsGeofencingFilterNodeConfiguration();
        configuration.setLatitudeKeyName("latitude");
        configuration.setLongitudeKeyName("longitude");
        configuration.setPerimeterType(PerimeterType.POLYGON);
        configuration.setFetchPerimeterInfoFromMessageMetadata(true);
        configuration.setPerimeterKeyName("ss_perimeter");
        return configuration;
    }
}
