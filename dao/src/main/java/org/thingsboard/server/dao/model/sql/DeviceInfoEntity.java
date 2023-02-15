package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.DeviceInfo;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceInfoEntity extends AbstractDeviceEntity<DeviceInfo> {

    public static final Map<String,String> deviceInfoColumnMap = new HashMap<>();
    static {
        deviceInfoColumnMap.put("customerTitle", "c.title");
        deviceInfoColumnMap.put("deviceProfileName", "p.name");
    }

    private String customerTitle;
    private boolean customerIsPublic;
    private String deviceProfileName;

    public DeviceInfoEntity() {
        super();
    }

    public DeviceInfoEntity(DeviceEntity deviceEntity,
                            String customerTitle,
                            Object customerAdditionalInfo,
                            String deviceProfileName) {
        super(deviceEntity);
        this.customerTitle = customerTitle;
        if (customerAdditionalInfo != null && ((JsonNode)customerAdditionalInfo).has("isPublic")) {
            this.customerIsPublic = ((JsonNode)customerAdditionalInfo).get("isPublic").asBoolean();
        } else {
            this.customerIsPublic = false;
        }
        this.deviceProfileName = deviceProfileName;
    }

    @Override
    public DeviceInfo toData() {
        return new DeviceInfo(super.toDevice(), customerTitle, customerIsPublic, deviceProfileName);
    }
}
