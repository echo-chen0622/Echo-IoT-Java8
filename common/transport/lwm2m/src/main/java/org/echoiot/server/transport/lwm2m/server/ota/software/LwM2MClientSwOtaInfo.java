package org.echoiot.server.transport.lwm2m.server.ota.software;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.transport.lwm2m.server.ota.LwM2MClientOtaInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class LwM2MClientSwOtaInfo extends LwM2MClientOtaInfo<LwM2MSoftwareUpdateStrategy, SoftwareUpdateState, SoftwareUpdateResult> {

    public LwM2MClientSwOtaInfo(String endpoint, String baseUrl, LwM2MSoftwareUpdateStrategy strategy) {
        super(endpoint, baseUrl, strategy);
    }

    @NotNull
    @JsonIgnore
    @Override
    public OtaPackageType getType() {
        return OtaPackageType.SOFTWARE;
    }


    public void update(@NotNull SoftwareUpdateResult result) {
        this.result = result;
        if (Objects.requireNonNull(result) == SoftwareUpdateResult.INITIAL) {//TODO: implement
        } else {
            failedPackageId = getPackageId(targetName, targetVersion);
        }
    }

}
