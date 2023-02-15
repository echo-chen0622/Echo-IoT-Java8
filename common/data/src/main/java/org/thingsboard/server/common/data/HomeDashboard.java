package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class HomeDashboard extends Dashboard {

    public static final String HIDE_DASHBOARD_TOOLBAR_DESCRIPTION = "Hide dashboard toolbar flag. Useful for rendering dashboards on mobile.";

    @ApiModelProperty(position = 10, value = HIDE_DASHBOARD_TOOLBAR_DESCRIPTION)
    private boolean hideDashboardToolbar;

    public HomeDashboard(Dashboard dashboard, boolean hideDashboardToolbar) {
        super(dashboard);
        this.hideDashboardToolbar = hideDashboardToolbar;
    }

}
