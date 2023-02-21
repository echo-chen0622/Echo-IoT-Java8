package org.echoiot.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.ResourceType;
import org.echoiot.server.common.data.TbResourceInfo;
import org.echoiot.server.common.data.id.TbResourceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.model.BaseSqlEntity;
import org.echoiot.server.dao.model.SearchTextEntity;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

import static org.echoiot.server.dao.model.ModelConstants.RESOURCE_KEY_COLUMN;
import static org.echoiot.server.dao.model.ModelConstants.RESOURCE_TABLE_NAME;
import static org.echoiot.server.dao.model.ModelConstants.RESOURCE_TENANT_ID_COLUMN;
import static org.echoiot.server.dao.model.ModelConstants.RESOURCE_TITLE_COLUMN;
import static org.echoiot.server.dao.model.ModelConstants.RESOURCE_TYPE_COLUMN;
import static org.echoiot.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = RESOURCE_TABLE_NAME)
public class TbResourceInfoEntity extends BaseSqlEntity<TbResourceInfo> implements SearchTextEntity<TbResourceInfo> {

    @Column(name = RESOURCE_TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = RESOURCE_TITLE_COLUMN)
    private String title;

    @Column(name = RESOURCE_TYPE_COLUMN)
    private String resourceType;

    @Column(name = RESOURCE_KEY_COLUMN)
    private String resourceKey;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    public TbResourceInfoEntity() {
    }

    public TbResourceInfoEntity(@NotNull TbResourceInfo resource) {
        if (resource.getId() != null) {
            this.id = resource.getId().getId();
        }
        this.createdTime = resource.getCreatedTime();
        this.tenantId = resource.getTenantId().getId();
        this.title = resource.getTitle();
        this.resourceType = resource.getResourceType().name();
        this.resourceKey = resource.getResourceKey();
        this.searchText = resource.getSearchText();
    }

    @NotNull
    @Override
    public TbResourceInfo toData() {
        @NotNull TbResourceInfo resource = new TbResourceInfo(new TbResourceId(id));
        resource.setCreatedTime(createdTime);
        resource.setTenantId(TenantId.fromUUID(tenantId));
        resource.setTitle(title);
        resource.setResourceType(ResourceType.valueOf(resourceType));
        resource.setResourceKey(resourceKey);
        resource.setSearchText(searchText);
        return resource;
    }

    @Override
    public String getSearchTextSource() {
        return title;
    }
}
