package org.thingsboard.server.common.data.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DeviceTypeFilter implements EntityFilter {

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.DEVICE_TYPE;
    }

    private String deviceType;

    private String deviceNameFilter;

}
