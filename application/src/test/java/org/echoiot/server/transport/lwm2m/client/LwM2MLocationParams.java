package org.echoiot.server.transport.lwm2m.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@Slf4j
@Data
public class LwM2MLocationParams {

    @Nullable
    private Float latitude;
    @Nullable
    private Float longitude;
    private Float scaleFactor = 1.0F;
    private final String locationPos = "50.4501:30.5234";
    private final Float locationScaleFactor = 1.0F;


   protected void getPos() {
        this.latitude = null;
        this.longitude = null;
        int c = locationPos.indexOf(':');
        this.latitude = Float.valueOf(locationPos.substring(0, c));
        this.longitude = Float.valueOf(locationPos.substring(c + 1));
    }


}
