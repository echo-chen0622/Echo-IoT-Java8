package org.thingsboard.server.common.data.device.profile;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;

import java.io.Serializable;

@ApiModel
@Data
public class AlarmRule implements Serializable {

    @Valid
    @ApiModelProperty(position = 1, value = "JSON object representing the alarm rule condition")
    private AlarmCondition condition;
    @ApiModelProperty(position = 2, value = "JSON object representing time interval during which the rule is active")
    private AlarmSchedule schedule;
    // Advanced
    @NoXss
    @ApiModelProperty(position = 3, value = "String value representing the additional details for an alarm rule")
    private String alarmDetails;
    @ApiModelProperty(position = 4, value = "JSON object with the dashboard Id representing the reference to alarm details dashboard used by mobile application")
    private DashboardId dashboardId;

}
