package org.thingsboard.rule.engine.geo;

import lombok.Data;

@Data
public class Perimeter {

    private PerimeterType perimeterType;

    //For Polygons
    private String polygonsDefinition;

    //For Circles
    private Double centerLatitude;
    private Double centerLongitude;
    private Double range;
    private RangeUnit rangeUnit;

}
