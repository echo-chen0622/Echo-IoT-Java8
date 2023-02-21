package org.echoiot.server.common.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@ApiModel
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SaveOtaPackageInfoRequest extends OtaPackageInfo {
    @ApiModelProperty(position = 16, value = "Indicates OTA Package uses url. Should be 'true' if uses url or 'false' if will be used data.", example = "true", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    boolean usesUrl;

    public SaveOtaPackageInfoRequest(@NotNull OtaPackageInfo otaPackageInfo, boolean usesUrl) {
        super(otaPackageInfo);
        this.usesUrl = usesUrl;
    }

    public SaveOtaPackageInfoRequest(@NotNull SaveOtaPackageInfoRequest saveOtaPackageInfoRequest) {
        super(saveOtaPackageInfoRequest);
        this.usesUrl = saveOtaPackageInfoRequest.isUsesUrl();
    }
}
