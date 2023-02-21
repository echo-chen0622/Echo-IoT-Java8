package org.echoiot.server.common.data.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DeviceTypeFilter implements EntityFilter {

    @NotNull
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.DEVICE_TYPE;
    }

    private String deviceType;

    private String deviceNameFilter;

}
