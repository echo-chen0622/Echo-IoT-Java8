package org.thingsboard.server.common.data.security.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@ApiModel
@Data
public class UserPasswordPolicy implements Serializable {

    @ApiModelProperty(position = 1, value = "Minimum number of symbols in the password." )
    private Integer minimumLength;
    @ApiModelProperty(position = 1, value = "Minimum number of uppercase letters in the password." )
    private Integer minimumUppercaseLetters;
    @ApiModelProperty(position = 1, value = "Minimum number of lowercase letters in the password." )
    private Integer minimumLowercaseLetters;
    @ApiModelProperty(position = 1, value = "Minimum number of digits in the password." )
    private Integer minimumDigits;
    @ApiModelProperty(position = 1, value = "Minimum number of special in the password." )
    private Integer minimumSpecialCharacters;
    @ApiModelProperty(position = 1, value = "Allow whitespaces")
    private Boolean allowWhitespaces = true;

    @ApiModelProperty(position = 1, value = "Password expiration period (days). Force expiration of the password." )
    private Integer passwordExpirationPeriodDays;
    @ApiModelProperty(position = 1, value = "Password reuse frequency (days). Disallow to use the same password for the defined number of days" )
    private Integer passwordReuseFrequencyDays;

}
