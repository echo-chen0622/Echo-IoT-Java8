package org.echoiot.server.common.data.sms.config;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@ApiModel
@Data
public class TwilioSmsProviderConfiguration implements SmsProviderConfiguration {

    @ApiModelProperty(position = 1, value = "Twilio account Sid.")
    private String accountSid;
    @ApiModelProperty(position = 2, value = "Twilio account Token.")
    private String accountToken;
    @ApiModelProperty(position = 3, value = "The number/id of a sender.")
    private String numberFrom;

    @NotNull
    @Override
    public SmsProviderType getType() {
        return SmsProviderType.TWILIO;
    }

}
