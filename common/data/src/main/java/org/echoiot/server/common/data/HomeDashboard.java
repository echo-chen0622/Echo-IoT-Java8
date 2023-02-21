package org.echoiot.server.common.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@ApiModel
@Data
public class HomeDashboard extends Dashboard {

    public static final String HIDE_DASHBOARD_TOOLBAR_DESCRIPTION = "Hide dashboard toolbar flag. Useful for rendering dashboards on mobile.";

    @ApiModelProperty(position = 10, value = HIDE_DASHBOARD_TOOLBAR_DESCRIPTION)
    private boolean hideDashboardToolbar;

    public HomeDashboard(@NotNull Dashboard dashboard, boolean hideDashboardToolbar) {
        super(dashboard);
        this.hideDashboardToolbar = hideDashboardToolbar;
    }

}
