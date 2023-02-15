package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SingleEntityFilter.class, name = "singleEntity"),
        @JsonSubTypes.Type(value = EntityListFilter.class, name = "entityList"),
        @JsonSubTypes.Type(value = EntityNameFilter.class, name = "entityName"),
        @JsonSubTypes.Type(value = EntityTypeFilter.class, name = "entityType"),
        @JsonSubTypes.Type(value = AssetTypeFilter.class, name = "assetType"),
        @JsonSubTypes.Type(value = DeviceTypeFilter.class, name = "deviceType"),
        @JsonSubTypes.Type(value = EdgeTypeFilter.class, name = "edgeType"),
        @JsonSubTypes.Type(value = EntityViewTypeFilter.class, name = "entityViewType"),
        @JsonSubTypes.Type(value = ApiUsageStateFilter.class, name = "apiUsageState"),
        @JsonSubTypes.Type(value = RelationsQueryFilter.class, name = "relationsQuery"),
        @JsonSubTypes.Type(value = AssetSearchQueryFilter.class, name = "assetSearchQuery"),
        @JsonSubTypes.Type(value = DeviceSearchQueryFilter.class, name = "deviceSearchQuery"),
        @JsonSubTypes.Type(value = EntityViewSearchQueryFilter.class, name = "entityViewSearchQuery"),
        @JsonSubTypes.Type(value = EdgeSearchQueryFilter.class, name = "edgeSearchQuery")})
public interface EntityFilter {

    @JsonIgnore
    EntityFilterType getType();
}
