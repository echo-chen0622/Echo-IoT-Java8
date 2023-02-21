package org.echoiot.server.dao.sql.query;

import lombok.Data;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.query.*;
import org.echoiot.server.dao.model.ModelConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class EntityKeyMapping {

    private static final Map<EntityType, Set<String>> allowedEntityFieldMap = new HashMap<>();
    private static final Map<String, String> entityFieldColumnMap = new HashMap<>();
    private static final Map<EntityType, Map<String, String>> aliases = new HashMap<>();

    public static final String CREATED_TIME = "createdTime";
    public static final String ENTITY_TYPE = "entityType";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String LABEL = "label";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String EMAIL = "email";
    public static final String TITLE = "title";
    public static final String REGION = "region";
    public static final String COUNTRY = "country";
    public static final String STATE = "state";
    public static final String CITY = "city";
    public static final String ADDRESS = "address";
    public static final String ADDRESS_2 = "address2";
    public static final String ZIP = "zip";
    public static final String PHONE = "phone";
    public static final String ADDITIONAL_INFO = "additionalInfo";
    public static final String RELATED_PARENT_ID = "parentId";

    public static final List<String> typedEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME, TYPE, ADDITIONAL_INFO);
    public static final List<String> widgetEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME);
    public static final List<String> commonEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME, ADDITIONAL_INFO);
    public static final List<String> dashboardEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, TITLE);
    public static final List<String> labeledEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME, TYPE, LABEL, ADDITIONAL_INFO);
    public static final List<String> contactBasedEntityFields = Arrays.asList(CREATED_TIME, ENTITY_TYPE, EMAIL, TITLE, COUNTRY, STATE, CITY, ADDRESS, ADDRESS_2, ZIP, PHONE, ADDITIONAL_INFO);

    public static final Set<String> apiUsageStateEntityFields =  new HashSet<>(Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME));
    public static final Set<String> commonEntityFieldsSet = new HashSet<>(commonEntityFields);
    public static final Set<String> relationQueryEntityFieldsSet = new HashSet<>(Arrays.asList(CREATED_TIME, ENTITY_TYPE, NAME, TYPE, LABEL, FIRST_NAME, LAST_NAME, EMAIL, REGION, TITLE, COUNTRY, STATE, CITY, ADDRESS, ADDRESS_2, ZIP, PHONE, ADDITIONAL_INFO, RELATED_PARENT_ID));

    static {
        allowedEntityFieldMap.put(EntityType.DEVICE, new HashSet<>(labeledEntityFields));
        allowedEntityFieldMap.put(EntityType.ASSET, new HashSet<>(labeledEntityFields));
        allowedEntityFieldMap.put(EntityType.ENTITY_VIEW, new HashSet<>(typedEntityFields));

        allowedEntityFieldMap.put(EntityType.TENANT, new HashSet<>(contactBasedEntityFields));
        allowedEntityFieldMap.get(EntityType.TENANT).add(REGION);
        allowedEntityFieldMap.put(EntityType.CUSTOMER, new HashSet<>(contactBasedEntityFields));

        allowedEntityFieldMap.put(EntityType.USER, new HashSet<>(Arrays.asList(CREATED_TIME, FIRST_NAME, LAST_NAME, EMAIL, ADDITIONAL_INFO)));

        allowedEntityFieldMap.put(EntityType.DASHBOARD, new HashSet<>(dashboardEntityFields));
        allowedEntityFieldMap.put(EntityType.RULE_CHAIN, new HashSet<>(commonEntityFields));
        allowedEntityFieldMap.put(EntityType.RULE_NODE, new HashSet<>(commonEntityFields));
        allowedEntityFieldMap.put(EntityType.WIDGET_TYPE, new HashSet<>(widgetEntityFields));
        allowedEntityFieldMap.put(EntityType.WIDGETS_BUNDLE, new HashSet<>(widgetEntityFields));
        allowedEntityFieldMap.put(EntityType.API_USAGE_STATE, apiUsageStateEntityFields);
        allowedEntityFieldMap.put(EntityType.DEVICE_PROFILE, Set.of(CREATED_TIME, NAME, TYPE));
        allowedEntityFieldMap.put(EntityType.ASSET_PROFILE, Set.of(CREATED_TIME, NAME));

        entityFieldColumnMap.put(CREATED_TIME, ModelConstants.CREATED_TIME_PROPERTY);
        entityFieldColumnMap.put(ENTITY_TYPE, ModelConstants.ENTITY_TYPE_PROPERTY);
        entityFieldColumnMap.put(REGION, ModelConstants.TENANT_REGION_PROPERTY);
        entityFieldColumnMap.put(NAME, "name");
        entityFieldColumnMap.put(TYPE, "type");
        entityFieldColumnMap.put(LABEL, "label");
        entityFieldColumnMap.put(FIRST_NAME, ModelConstants.USER_FIRST_NAME_PROPERTY);
        entityFieldColumnMap.put(LAST_NAME, ModelConstants.USER_LAST_NAME_PROPERTY);
        entityFieldColumnMap.put(EMAIL, ModelConstants.EMAIL_PROPERTY);
        entityFieldColumnMap.put(TITLE, ModelConstants.TITLE_PROPERTY);
        entityFieldColumnMap.put(COUNTRY, ModelConstants.COUNTRY_PROPERTY);
        entityFieldColumnMap.put(STATE, ModelConstants.STATE_PROPERTY);
        entityFieldColumnMap.put(CITY, ModelConstants.CITY_PROPERTY);
        entityFieldColumnMap.put(ADDRESS, ModelConstants.ADDRESS_PROPERTY);
        entityFieldColumnMap.put(ADDRESS_2, ModelConstants.ADDRESS2_PROPERTY);
        entityFieldColumnMap.put(ZIP, ModelConstants.ZIP_PROPERTY);
        entityFieldColumnMap.put(PHONE, ModelConstants.PHONE_PROPERTY);
        entityFieldColumnMap.put(ADDITIONAL_INFO, ModelConstants.ADDITIONAL_INFO_PROPERTY);
        entityFieldColumnMap.put(RELATED_PARENT_ID, "parent_id");

        @NotNull Map<String, String> contactBasedAliases = new HashMap<>();
        contactBasedAliases.put(NAME, TITLE);
        contactBasedAliases.put(LABEL, TITLE);
        aliases.put(EntityType.TENANT, contactBasedAliases);
        aliases.put(EntityType.CUSTOMER, contactBasedAliases);
        aliases.put(EntityType.DASHBOARD, contactBasedAliases);
        @NotNull Map<String, String> commonEntityAliases = new HashMap<>();
        commonEntityAliases.put(TITLE, NAME);
        aliases.put(EntityType.DEVICE, commonEntityAliases);
        aliases.put(EntityType.ASSET, commonEntityAliases);
        aliases.put(EntityType.ENTITY_VIEW, commonEntityAliases);
        aliases.put(EntityType.WIDGETS_BUNDLE, commonEntityAliases);

        @NotNull Map<String, String> userEntityAliases = new HashMap<>();
        userEntityAliases.put(TITLE, EMAIL);
        userEntityAliases.put(LABEL, EMAIL);
        userEntityAliases.put(NAME, EMAIL);
        aliases.put(EntityType.USER, userEntityAliases);
    }

    private int index;
    private String alias;
    private boolean isLatest;
    private boolean isSelection;
    private boolean isSearchable;
    private boolean isSortOrder;
    private boolean ignore = false;
    private List<KeyFilter> keyFilters;
    private EntityKey entityKey;
    private int paramIdx = 0;

    public boolean hasFilter() {
        return keyFilters != null && !keyFilters.isEmpty();
    }

    public String getValueAlias() {
        if (entityKey.getType().equals(EntityKeyType.ENTITY_FIELD)) {
            return alias;
        } else {
            return alias + "_value";
        }
    }

    @NotNull
    public String getTsAlias() {
        return alias + "_ts";
    }

    public String toSelection(@NotNull EntityFilterType filterType, @NotNull EntityType entityType) {
        if (entityKey.getType().equals(EntityKeyType.ENTITY_FIELD)) {
            if (entityKey.getKey().equals("entityType") && !filterType.equals(EntityFilterType.RELATIONS_QUERY)) {
                return String.format("'%s' as %s", entityType.name(), getValueAlias());
            } else {
                Set<String> existingEntityFields = getExistingEntityFields(filterType, entityType);
                String alias = getEntityFieldAlias(filterType, entityType);
                if (existingEntityFields.contains(alias)) {
                    String column = entityFieldColumnMap.get(alias);
                    return String.format("cast(e.%s as varchar) as %s", column, getValueAlias());
                } else {
                    return String.format("'' as %s", getValueAlias());
                }
            }
        } else if (entityKey.getType().equals(EntityKeyType.TIME_SERIES)) {
            return buildTimeSeriesSelection();
        } else {
            return buildAttributeSelection();
        }
    }

    private String getEntityFieldAlias(@NotNull EntityFilterType filterType, EntityType entityType) {
        String alias;
        if (filterType.equals(EntityFilterType.RELATIONS_QUERY)) {
            alias = entityKey.getKey();
        } else {
            alias = getAliasByEntityKeyAndType(entityKey.getKey(), entityType);
        }
        return alias;
    }

    private Set<String> getExistingEntityFields(@NotNull EntityFilterType filterType, EntityType entityType) {
        Set<String> existingEntityFields;
        if (filterType.equals(EntityFilterType.RELATIONS_QUERY)) {
            existingEntityFields = relationQueryEntityFieldsSet;
        } else {
            existingEntityFields = allowedEntityFieldMap.get(entityType);
            if (existingEntityFields == null) {
                existingEntityFields = commonEntityFieldsSet;
            }
        }
        return existingEntityFields;
    }

    private String getAliasByEntityKeyAndType(String key, EntityType entityType) {
        @Nullable String alias;
        Map<String, String> entityAliases = aliases.get(entityType);
        if (entityAliases != null) {
            alias = entityAliases.get(key);
        } else {
            alias = null;
        }
        if (alias == null) {
            alias = key;
        }
        return alias;
    }

    @NotNull
    public Stream<String> toQueries(@NotNull QueryContext ctx, @NotNull EntityFilterType filterType) {
        if (hasFilter()) {
            String keyAlias = entityKey.getType().equals(EntityKeyType.ENTITY_FIELD) ? "e" : alias;
            return keyFilters.stream().map(keyFilter ->
                    this.buildKeyQuery(ctx, keyAlias, keyFilter, filterType));
        } else {
            return Stream.empty();
        }
    }

    public String toLatestJoin(@NotNull QueryContext ctx, @NotNull EntityFilter entityFilter, @NotNull EntityType entityType) {
        String entityTypeStr;
        if (entityFilter.getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            entityTypeStr = "entities.entity_type";
        } else {
            entityTypeStr = "'" + entityType.name() + "'";
        }
        ctx.addStringParameter(getKeyId(), entityKey.getKey());
        String filterQuery = toQueries(ctx, entityFilter.getType())
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(" and "));
        if (StringUtils.isNotEmpty(filterQuery)) {
            filterQuery = " AND (" + filterQuery + ")";
        }
        if (entityKey.getType().equals(EntityKeyType.TIME_SERIES)) {
            @NotNull String join = (hasFilter() && hasFilterValues(ctx)) ? "inner join" : "left join";
            return String.format("%s ts_kv_latest %s ON %s.entity_id=entities.id AND %s.key = (select key_id from ts_kv_dictionary where key = :%s_key_id) %s",
                    join, alias, alias, alias, alias, filterQuery);
        } else {
            String query;
            if (!entityKey.getType().equals(EntityKeyType.ATTRIBUTE)) {
                @NotNull String join = (hasFilter() && hasFilterValues(ctx)) ? "inner join" : "left join";
                query = String.format("%s attribute_kv %s ON %s.entity_id=entities.id AND %s.entity_type=%s AND %s.attribute_key=:%s_key_id ",
                        join, alias, alias, alias, entityTypeStr, alias, alias);
                String scope;
                if (entityKey.getType().equals(EntityKeyType.CLIENT_ATTRIBUTE)) {
                    scope = DataConstants.CLIENT_SCOPE;
                } else if (entityKey.getType().equals(EntityKeyType.SHARED_ATTRIBUTE)) {
                    scope = DataConstants.SHARED_SCOPE;
                } else {
                    scope = DataConstants.SERVER_SCOPE;
                }
                query = String.format("%s AND %s.attribute_type='%s' %s", query, alias, scope, filterQuery);
            } else {
                @NotNull String join = (hasFilter() && hasFilterValues(ctx)) ? "join LATERAL" : "left join LATERAL";
                query = String.format("%s (select * from attribute_kv %s WHERE %s.entity_id=entities.id AND %s.entity_type=%s AND %s.attribute_key=:%s_key_id %s " +
                                "ORDER BY %s.last_update_ts DESC limit 1) as %s ON true",
                        join, alias, alias, alias, entityTypeStr, alias, alias, filterQuery, alias, alias);
            }
            return query;
        }
    }

    private boolean hasFilterValues(@NotNull QueryContext ctx) {
        return Arrays.stream(ctx.getParameterNames()).anyMatch(parameterName -> {
            return !parameterName.equals(getKeyId()) && parameterName.startsWith(alias);
        });
    }

    @NotNull
    private String getKeyId() {
        return alias + "_key_id";
    }

    public static String buildSelections(@NotNull List<EntityKeyMapping> mappings, @NotNull EntityFilterType filterType, @NotNull EntityType entityType) {
        return mappings.stream().map(mapping -> mapping.toSelection(filterType, entityType)).collect(
                Collectors.joining(", "));
    }

    public static String buildLatestJoins(@NotNull QueryContext ctx, @NotNull EntityFilter entityFilter, @NotNull EntityType entityType, @NotNull List<EntityKeyMapping> latestMappings, boolean countQuery) {
        return latestMappings.stream()
                .filter(mapping -> !countQuery || mapping.hasFilter())
                .map(mapping -> mapping.toLatestJoin(ctx, entityFilter, entityType))
                .collect(Collectors.joining(" "));
    }

    public static String buildQuery(@NotNull QueryContext ctx, @NotNull List<EntityKeyMapping> mappings, @NotNull EntityFilterType filterType) {
        return mappings.stream()
                .flatMap(mapping -> mapping.toQueries(ctx, filterType))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(" AND "));
    }

    @NotNull
    public static List<EntityKeyMapping> prepareKeyMapping(@NotNull EntityDataQuery query) {
        @NotNull List<EntityKey> entityFields = query.getEntityFields() != null ? query.getEntityFields() : Collections.emptyList();
        @NotNull List<EntityKey> latestValues = query.getLatestValues() != null ? query.getLatestValues() : Collections.emptyList();
        @NotNull Map<EntityKey, List<KeyFilter>> filters =
                query.getKeyFilters() != null ?
                        query.getKeyFilters().stream().collect(Collectors.groupingBy(KeyFilter::getKey)) : Collections.emptyMap();
        EntityDataSortOrder sortOrder = query.getPageLink().getSortOrder();
        EntityKey sortOrderKey = sortOrder != null ? sortOrder.getKey() : null;
        int index = 2;
        @NotNull List<EntityKeyMapping> entityFieldsMappings = entityFields.stream().map(
                key -> {
                    @NotNull EntityKeyMapping mapping = new EntityKeyMapping();
                    mapping.setLatest(false);
                    mapping.setSelection(true);
                    mapping.setSearchable(!key.getKey().equals(ADDITIONAL_INFO));
                    mapping.setEntityKey(key);
                    return mapping;
                }
                                                                                        ).collect(Collectors.toList());
        @NotNull List<EntityKeyMapping> latestMappings = latestValues.stream().map(
                key -> {
                    @NotNull EntityKeyMapping mapping = new EntityKeyMapping();
                    mapping.setLatest(true);
                    mapping.setSearchable(true);
                    mapping.setSelection(true);
                    mapping.setEntityKey(key);
                    return mapping;
                }
                                                                                  ).collect(Collectors.toList());
        if (sortOrderKey != null) {
            Optional<EntityKeyMapping> existing;
            if (sortOrderKey.getType().equals(EntityKeyType.ENTITY_FIELD)) {
                existing =
                        entityFieldsMappings.stream().filter(mapping -> mapping.entityKey.equals(sortOrderKey)).findFirst();
            } else {
                existing =
                        latestMappings.stream().filter(mapping -> mapping.entityKey.equals(sortOrderKey)).findFirst();
            }
            if (existing.isPresent()) {
                existing.get().setSortOrder(true);
            } else {
                @NotNull EntityKeyMapping sortOrderMapping = new EntityKeyMapping();
                sortOrderMapping.setLatest(!sortOrderKey.getType().equals(EntityKeyType.ENTITY_FIELD));
                sortOrderMapping.setSelection(true);
                sortOrderMapping.setEntityKey(sortOrderKey);
                sortOrderMapping.setSortOrder(true);
                sortOrderMapping.setIgnore(true);
                if (sortOrderKey.getType().equals(EntityKeyType.ENTITY_FIELD)) {
                    entityFieldsMappings.add(sortOrderMapping);
                } else {
                    latestMappings.add(sortOrderMapping);
                }
            }
        }
        @NotNull List<EntityKeyMapping> mappings = new ArrayList<>();
        mappings.addAll(entityFieldsMappings);
        mappings.addAll(latestMappings);
        for (@NotNull EntityKeyMapping mapping : mappings) {
            mapping.setIndex(index);
            mapping.setAlias(String.format("alias%s", index));
            mapping.setKeyFilters(filters.remove(mapping.entityKey));
            if (mapping.getEntityKey().getType().equals(EntityKeyType.ENTITY_FIELD)) {
                index++;
            } else {
                index += 2;
            }
        }
        if (!filters.isEmpty()) {
            for (@NotNull EntityKey filterField : filters.keySet()) {
                @NotNull EntityKeyMapping mapping = new EntityKeyMapping();
                mapping.setIndex(index);
                mapping.setAlias(String.format("alias%s", index));
                mapping.setKeyFilters(filters.get(filterField));
                mapping.setLatest(!filterField.getType().equals(EntityKeyType.ENTITY_FIELD));
                mapping.setSelection(false);
                mapping.setEntityKey(filterField);
                mappings.add(mapping);
                index += 1;
            }
        }

        return mappings;
    }

    @NotNull
    public static List<EntityKeyMapping> prepareEntityCountKeyMapping(@NotNull EntityCountQuery query) {
        @NotNull Map<EntityKey, List<KeyFilter>> filters =
                query.getKeyFilters() != null ?
                        query.getKeyFilters().stream().collect(Collectors.groupingBy(KeyFilter::getKey)) : Collections.emptyMap();
        int index = 2;
        @NotNull List<EntityKeyMapping> mappings = new ArrayList<>();
        if (!filters.isEmpty()) {
            for (@NotNull EntityKey filterField : filters.keySet()) {
                @NotNull EntityKeyMapping mapping = new EntityKeyMapping();
                mapping.setIndex(index);
                mapping.setAlias(String.format("alias%s", index));
                mapping.setKeyFilters(filters.get(filterField));
                mapping.setLatest(!filterField.getType().equals(EntityKeyType.ENTITY_FIELD));
                mapping.setSelection(false);
                mapping.setEntityKey(filterField);
                mappings.add(mapping);
                index += 1;
            }
        }

        return mappings;
    }


    @NotNull
    private String buildAttributeSelection() {
        return buildTimeSeriesOrAttrSelection(true);
    }

    @NotNull
    private String buildTimeSeriesSelection() {
        return buildTimeSeriesOrAttrSelection(false);
    }

    @NotNull
    private String buildTimeSeriesOrAttrSelection(boolean attr) {
        String attrValAlias = getValueAlias();
        @NotNull String attrTsAlias = getTsAlias();
        String attrValSelection =
                String.format("(coalesce(cast(%s.bool_v as varchar), '') || " +
                        "coalesce(%s.str_v, '') || " +
                        "coalesce(cast(%s.long_v as varchar), '') || " +
                        "coalesce(cast(%s.dbl_v as varchar), '') || " +
                        "coalesce(cast(%s.json_v as varchar), '')) as %s", alias, alias, alias, alias, alias, attrValAlias);
        String attrTsSelection = String.format("%s.%s as %s", alias, attr ? "last_update_ts" : "ts", attrTsAlias);
        if (this.isSortOrder) {
            @NotNull String attrNumAlias = getSortOrderNumAlias();
            @NotNull String attrVarcharAlias = getSortOrderStrAlias();
            String attrSortOrderSelection =
                    String.format("coalesce(%s.dbl_v, cast(%s.long_v as double precision), (case when %s.bool_v then 1 else 0 end)) %s," +
                            "coalesce(%s.str_v, cast(%s.json_v as varchar), '') %s", alias, alias, alias, attrNumAlias, alias, alias, attrVarcharAlias);
            return String.join(", ", attrValSelection, attrTsSelection, attrSortOrderSelection);
        } else {
            return String.join(", ", attrValSelection, attrTsSelection);
        }
    }

    @NotNull
    public String getSortOrderStrAlias() {
        return getValueAlias() + "_so_varchar";
    }

    @NotNull
    public String getSortOrderNumAlias() {
        return getValueAlias() + "_so_num";
    }

    private String buildKeyQuery(@NotNull QueryContext ctx, String alias, @NotNull KeyFilter keyFilter,
                                 @NotNull EntityFilterType filterType) {
        return this.buildPredicateQuery(ctx, alias, keyFilter.getKey(), keyFilter.getPredicate(), filterType);
    }

    @Nullable
    private String buildPredicateQuery(@NotNull QueryContext ctx, String alias, @NotNull EntityKey key,
                                       @NotNull KeyFilterPredicate predicate, @NotNull EntityFilterType filterType) {
        if (predicate.getType().equals(FilterPredicateType.COMPLEX)) {
            return this.buildComplexPredicateQuery(ctx, alias, key, (ComplexFilterPredicate) predicate, filterType);
        } else {
            return this.buildSimplePredicateQuery(ctx, alias, key, predicate, filterType);
        }
    }

    @NotNull
    private String buildComplexPredicateQuery(@NotNull QueryContext ctx, String alias, @NotNull EntityKey key,
                                              @NotNull ComplexFilterPredicate predicate, @NotNull EntityFilterType filterType) {
        String result = predicate.getPredicates().stream()
                .map(keyFilterPredicate -> this.buildPredicateQuery(ctx, alias, key, keyFilterPredicate, filterType))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.joining(" " + predicate.getOperation().name() + " "));
        if (!result.trim().isEmpty()) {
            result = "( " + result + " )";
        }
        return result;
    }

    @Nullable
    private String buildSimplePredicateQuery(@NotNull QueryContext ctx, String alias, @NotNull EntityKey key,
                                             @NotNull KeyFilterPredicate predicate, @NotNull EntityFilterType filterType) {
        if (key.getType().equals(EntityKeyType.ENTITY_FIELD)) {
            Set<String> existingEntityFields = getExistingEntityFields(filterType, ctx.getEntityType());
            String entityFieldAlias = getEntityFieldAlias(filterType, ctx.getEntityType());
            @Nullable String column = null;
            if (existingEntityFields.contains(entityFieldAlias)) {
                column = entityFieldColumnMap.get(entityFieldAlias);
            }
            if (column != null) {
                String field = alias + "." + column;
                if (predicate.getType().equals(FilterPredicateType.NUMERIC)) {
                    return this.buildNumericPredicateQuery(ctx, field, (NumericFilterPredicate) predicate);
                } else if (predicate.getType().equals(FilterPredicateType.STRING)) {
                    if (key.getKey().equals("entityType") && !filterType.equals(EntityFilterType.RELATIONS_QUERY)) {
                        field = ctx.getEntityType().toString();
                        return this.buildStringPredicateQuery(ctx, field, (StringFilterPredicate) predicate)
                                .replace("lower(" + field, "lower('" + field + "'")
                                .replace(field + " ", "'" + field + "' ");
                    } else {
                        return this.buildStringPredicateQuery(ctx, field, (StringFilterPredicate) predicate);
                    }
                } else {
                    return this.buildBooleanPredicateQuery(ctx, field, (BooleanFilterPredicate) predicate);
                }
            } else {
                return null;
            }
        } else {
            if (predicate.getType().equals(FilterPredicateType.NUMERIC)) {
                String longQuery = this.buildNumericPredicateQuery(ctx, alias + ".long_v", (NumericFilterPredicate) predicate);
                String doubleQuery = this.buildNumericPredicateQuery(ctx, alias + ".dbl_v", (NumericFilterPredicate) predicate);
                return String.format("(%s or %s)", longQuery, doubleQuery);
            } else {
                @NotNull String column = predicate.getType().equals(FilterPredicateType.STRING) ? "str_v" : "bool_v";
                @NotNull String field = alias + "." + column;
                if (predicate.getType().equals(FilterPredicateType.STRING)) {
                    return this.buildStringPredicateQuery(ctx, field, (StringFilterPredicate) predicate);
                } else {
                    return this.buildBooleanPredicateQuery(ctx, field, (BooleanFilterPredicate) predicate);
                }
            }
        }
    }

    private String buildStringPredicateQuery(@NotNull QueryContext ctx, @NotNull String field, @NotNull StringFilterPredicate stringFilterPredicate) {
        String operationField = field;
        @NotNull String paramName = getNextParameterName(field);
        String value = stringFilterPredicate.getValue().getValue();
        if (value.isEmpty()) {
            return "";
        }
        String stringOperationQuery = "";
        if (stringFilterPredicate.isIgnoreCase()) {
            value = value.toLowerCase();
            operationField = String.format("lower(%s)", operationField);
        }
        switch (stringFilterPredicate.getOperation()) {
            case EQUAL:
                stringOperationQuery = String.format("%s = :%s)", operationField, paramName);
                break;
            case NOT_EQUAL:
                stringOperationQuery = String.format("%s != :%s or %s is null)", operationField, paramName, operationField);
                break;
            case STARTS_WITH:
                value += "%";
                stringOperationQuery = String.format("%s like :%s)", operationField, paramName);
                break;
            case ENDS_WITH:
                value = "%" + value;
                stringOperationQuery = String.format("%s like :%s)", operationField, paramName);
                break;
            case CONTAINS:
                value = "%" + value + "%";
                stringOperationQuery = String.format("%s like :%s)", operationField, paramName);
                break;
            case NOT_CONTAINS:
                value = "%" + value + "%";
                stringOperationQuery = String.format("%s not like :%s or %s is null)", operationField, paramName, operationField);
                break;
            case IN:
                stringOperationQuery = String.format("%s in (:%s))", operationField, paramName);
                break;
            case NOT_IN:
                stringOperationQuery = String.format("%s not in (:%s))", operationField, paramName);
                break;
        }
        switch (stringFilterPredicate.getOperation()) {
            case IN:
            case NOT_IN:
                ctx.addStringListParameter(paramName, getListValuesWithoutQuote(value));
                break;
            default:
                ctx.addStringParameter(paramName, value);
        }
        return String.format("((%s is not null and %s)", field, stringOperationQuery);
    }

    @NotNull
    protected List<String> getListValuesWithoutQuote(@NotNull String value) {
        @NotNull List<String> splitValues = List.of(value.trim().split("\\s*,\\s*"));
        @NotNull List<String> result = new ArrayList<>();
        char lastWayInputValue = '#';
        for (@NotNull String str : splitValues) {
            char startWith = str.charAt(0);
            char endWith = str.charAt(str.length() - 1);

            // if first value is not quote, so we return values after split
            if (startWith != '\'' && startWith != '"') return splitValues;

            // if value is not in quote, so we return values after split
            if (startWith != endWith) return splitValues;

            // if different way values, so don't replace quote and return values after split
            if (lastWayInputValue != '#' && startWith != lastWayInputValue) return splitValues;

            result.add(str.substring(1, str.length() - 1));
            lastWayInputValue = startWith;
        }
        return result;
    }

    private String buildNumericPredicateQuery(@NotNull QueryContext ctx, @NotNull String field, @NotNull NumericFilterPredicate numericFilterPredicate) {
        @NotNull String paramName = getNextParameterName(field);
        ctx.addDoubleParameter(paramName, numericFilterPredicate.getValue().getValue());
        String numericOperationQuery = "";
        switch (numericFilterPredicate.getOperation()) {
            case EQUAL:
                numericOperationQuery = String.format("%s = :%s", field, paramName);
                break;
            case NOT_EQUAL:
                numericOperationQuery = String.format("%s != :%s", field, paramName);
                break;
            case GREATER:
                numericOperationQuery = String.format("%s > :%s", field, paramName);
                break;
            case GREATER_OR_EQUAL:
                numericOperationQuery = String.format("%s >= :%s", field, paramName);
                break;
            case LESS:
                numericOperationQuery = String.format("%s < :%s", field, paramName);
                break;
            case LESS_OR_EQUAL:
                numericOperationQuery = String.format("%s <= :%s", field, paramName);
                break;
        }
        return String.format("(%s is not null and %s)", field, numericOperationQuery);
    }

    private String buildBooleanPredicateQuery(@NotNull QueryContext ctx, @NotNull String field,
                                              @NotNull BooleanFilterPredicate booleanFilterPredicate) {
        @NotNull String paramName = getNextParameterName(field);
        ctx.addBooleanParameter(paramName, booleanFilterPredicate.getValue().getValue());
        String booleanOperationQuery = "";
        switch (booleanFilterPredicate.getOperation()) {
            case EQUAL:
                booleanOperationQuery = String.format("%s = :%s", field, paramName);
                break;
            case NOT_EQUAL:
                booleanOperationQuery = String.format("%s != :%s", field, paramName);
                break;
        }
        return String.format("(%s is not null and %s)", field, booleanOperationQuery);
    }

    @NotNull
    private String getNextParameterName(@NotNull String field) {
        paramIdx++;
        return field.replace(".", "_") + "_" + paramIdx;
    }
}
