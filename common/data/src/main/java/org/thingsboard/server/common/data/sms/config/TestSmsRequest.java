package org.thingsboard.server.common.data.sms.config;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class TestSmsRequest {

    @ApiModelProperty(position = 1, value = "The SMS provider configuration")
    private SmsProviderConfiguration providerConfiguration;
    @ApiModelProperty(position = 2, value = "The phone number or other identifier to specify as a recipient of the SMS.")
    private String numberTo;
    @ApiModelProperty(position = 3, value = "The test message")
    private String message;

}
