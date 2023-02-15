package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;

import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "image")
public class DeviceProfileInfo extends EntityInfo {

    @ApiModelProperty(position = 3, value = "Either URL or Base64 data of the icon. Used in the mobile application to visualize set of device profiles in the grid view. ")
    private final String image;
    @ApiModelProperty(position = 4, value = "Reference to the dashboard. Used in the mobile application to open the default dashboard when user navigates to device details.")
    private final DashboardId defaultDashboardId;
    @ApiModelProperty(position = 5, value = "Type of the profile. Always 'DEFAULT' for now. Reserved for future use.")
    private final DeviceProfileType type;
    @ApiModelProperty(position = 6, value = "Type of the transport used to connect the device. Default transport supports HTTP, CoAP and MQTT.")
    private final DeviceTransportType transportType;

    @JsonCreator
    public DeviceProfileInfo(@JsonProperty("id") EntityId id,
                             @JsonProperty("name") String name,
                             @JsonProperty("image") String image,
                             @JsonProperty("defaultDashboardId") DashboardId defaultDashboardId,
                             @JsonProperty("type") DeviceProfileType type,
                             @JsonProperty("transportType") DeviceTransportType transportType) {
        super(id, name);
        this.image = image;
        this.defaultDashboardId = defaultDashboardId;
        this.type = type;
        this.transportType = transportType;
    }

    public DeviceProfileInfo(UUID uuid, String name, String image, UUID defaultDashboardId, DeviceProfileType type, DeviceTransportType transportType) {
        super(EntityIdFactory.getByTypeAndUuid(EntityType.DEVICE_PROFILE, uuid), name);
        this.image = image;
        this.defaultDashboardId = defaultDashboardId != null ? new DashboardId(defaultDashboardId) : null;
        this.type = type;
        this.transportType = transportType;
    }

    public DeviceProfileInfo(DeviceProfile profile) {
        this(profile.getId(), profile.getName(), profile.getImage(), profile.getDefaultDashboardId(),
                profile.getType(), profile.getTransportType());
    }

}
