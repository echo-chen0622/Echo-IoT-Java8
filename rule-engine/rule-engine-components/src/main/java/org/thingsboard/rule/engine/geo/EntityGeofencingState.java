package org.thingsboard.rule.engine.geo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EntityGeofencingState {

    private boolean inside;
    private long stateSwitchTime;
    private boolean stayed;

}
