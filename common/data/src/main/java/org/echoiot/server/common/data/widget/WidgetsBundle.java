package org.echoiot.server.common.data.widget;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.echoiot.server.common.data.validation.Length;
import org.echoiot.server.common.data.validation.NoXss;
import org.echoiot.server.common.data.ExportableEntity;
import org.echoiot.server.common.data.HasName;
import org.echoiot.server.common.data.HasTenantId;
import org.echoiot.server.common.data.SearchTextBased;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.WidgetsBundleId;
import org.jetbrains.annotations.NotNull;

@ApiModel
@EqualsAndHashCode(callSuper = true)
public class WidgetsBundle extends SearchTextBased<WidgetsBundleId> implements HasName, HasTenantId, ExportableEntity<WidgetsBundleId> {

    private static final long serialVersionUID = -7627368878362410489L;

    @Getter
    @Setter
    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;

    @NoXss
    @Length(fieldName = "alias")
    @Getter
    @Setter
    @ApiModelProperty(position = 4, value = "Unique alias that is used in widget types as a reference widget bundle", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String alias;

    @NoXss
    @Length(fieldName = "title")
    @Getter
    @Setter
    @ApiModelProperty(position = 5, value = "Title used in search and UI", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String title;

    @Length(fieldName = "image", max = 1000000)
    @Getter
    @Setter
    @ApiModelProperty(position = 6, value = "Base64 encoded thumbnail", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String image;

    @NoXss
    @Length(fieldName = "description")
    @Getter
    @Setter
    @ApiModelProperty(position = 7, value = "Description", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String description;

    @Getter
    @Setter
    private WidgetsBundleId externalId;

    public WidgetsBundle() {
        super();
    }

    public WidgetsBundle(WidgetsBundleId id) {
        super(id);
    }

    public WidgetsBundle(@NotNull WidgetsBundle widgetsBundle) {
        super(widgetsBundle);
        this.tenantId = widgetsBundle.getTenantId();
        this.alias = widgetsBundle.getAlias();
        this.title = widgetsBundle.getTitle();
        this.image = widgetsBundle.getImage();
        this.description = widgetsBundle.getDescription();
        this.externalId = widgetsBundle.getExternalId();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the Widget Bundle Id. " +
            "Specify this field to update the Widget Bundle. " +
            "Referencing non-existing Widget Bundle Id will cause error. " +
            "Omit this field to create new Widget Bundle." )
    @Override
    public WidgetsBundleId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the Widget Bundle creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    public String getSearchText() {
        return getTitle();
    }

    @ApiModelProperty(position = 3, value = "Same as title of the Widget Bundle. Read-only field. Update the 'title' to change the 'name' of the Widget Bundle.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }

    @NotNull
    @Override
    public String toString() {
        String sb = "WidgetsBundle{" + "tenantId=" + tenantId + ", alias='" + alias + '\'' + ", title='" + title + '\'' + ", description='" + description + '\'' + '}';
        return sb;
    }

}
