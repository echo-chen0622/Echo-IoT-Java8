package org.echoiot.server.transport.lwm2m.server.ota.firmware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.transport.lwm2m.server.ota.LwM2MClientOtaInfo;
import org.jetbrains.annotations.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class LwM2MClientFwOtaInfo extends LwM2MClientOtaInfo<LwM2MFirmwareUpdateStrategy, FirmwareUpdateState, FirmwareUpdateResult> {

    private Integer deliveryMethod;

    public LwM2MClientFwOtaInfo(String endpoint, String baseUrl, LwM2MFirmwareUpdateStrategy strategy) {
        super(endpoint, baseUrl, strategy);
    }

    @NotNull
    @JsonIgnore
    @Override
    public OtaPackageType getType() {
        return OtaPackageType.FIRMWARE;
    }

    public void update(@NotNull FirmwareUpdateResult result) {
        this.result = result;
        switch (result) {
            case INITIAL:
                break;
            case UPDATE_SUCCESSFULLY:
                retryAttempts = 0;
                break;
            default:
                failedPackageId = getPackageId(targetName, targetVersion);
                break;
        }
    }

}
