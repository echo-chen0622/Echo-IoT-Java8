package org.echoiot.server.common.data.sms.config;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@ApiModel
@Data
public class AwsSnsSmsProviderConfiguration implements SmsProviderConfiguration {

    @ApiModelProperty(position = 1, value = "The AWS SNS Access Key ID.")
    private String accessKeyId;
    @ApiModelProperty(position = 2, value = "The AWS SNS Access Key.")
    private String secretAccessKey;
    @ApiModelProperty(position = 3, value = "The AWS region.")
    private String region;

    @NotNull
    @Override
    public SmsProviderType getType() {
        return SmsProviderType.AWS_SNS;
    }

}
