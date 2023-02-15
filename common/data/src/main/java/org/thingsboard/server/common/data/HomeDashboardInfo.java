package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.DashboardId;

@ApiModel
@Data
@AllArgsConstructor
public class HomeDashboardInfo {
    @ApiModelProperty(position = 1, value = "JSON object with the dashboard Id.")
    private DashboardId dashboardId;
    @ApiModelProperty(position = 1, value = HomeDashboard.HIDE_DASHBOARD_TOOLBAR_DESCRIPTION)
    private boolean hideDashboardToolbar;
}
