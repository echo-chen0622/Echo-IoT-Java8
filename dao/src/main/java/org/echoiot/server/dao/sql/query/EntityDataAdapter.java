package org.echoiot.server.dao.sql.query;

import org.apache.commons.lang3.math.NumberUtils;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class EntityDataAdapter {

    @NotNull
    public static PageData<EntityData> createEntityData(@NotNull EntityDataPageLink pageLink,
                                                        @NotNull List<EntityKeyMapping> selectionMapping,
                                                        @NotNull List<Map<String, Object>> rows,
                                                        int totalElements) {
        int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        boolean hasNext = pageLink.getPageSize() > 0 && totalElements > startIndex + rows.size();
        @NotNull List<EntityData> entitiesData = convertListToEntityData(rows, selectionMapping);
        return new PageData<>(entitiesData, totalPages, totalElements, hasNext);
    }

    @NotNull
    private static List<EntityData> convertListToEntityData(@NotNull List<Map<String, Object>> result, @NotNull List<EntityKeyMapping> selectionMapping) {
        return result.stream().map(row -> toEntityData(row, selectionMapping)).collect(Collectors.toList());
    }

    @NotNull
    private static EntityData toEntityData(@NotNull Map<String, Object> row, @NotNull List<EntityKeyMapping> selectionMapping) {
        UUID id = (UUID)row.get("id");
        @NotNull EntityType entityType = EntityType.valueOf((String) row.get("entity_type"));
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, id);
        @NotNull Map<EntityKeyType, Map<String, TsValue>> latest = new HashMap<>();
        //Maybe avoid empty hashmaps?
        @NotNull EntityData entityData = new EntityData(entityId, latest, new HashMap<>(), new HashMap<>());
        for (@NotNull EntityKeyMapping mapping : selectionMapping) {
            if (!mapping.isIgnore()) {
                EntityKey entityKey = mapping.getEntityKey();
                Object value = row.get(mapping.getValueAlias());
                String strValue;
                long ts;
                if (entityKey.getType().equals(EntityKeyType.ENTITY_FIELD)) {
                    strValue = value != null ? value.toString() : "";
                    ts = System.currentTimeMillis();
                } else {
                    strValue = convertValue(value);
                    Object tsObject = row.get(mapping.getTsAlias());
                    ts = tsObject != null ? Long.parseLong(tsObject.toString()) : 0;
                }
                @NotNull TsValue tsValue = new TsValue(ts, strValue);
                latest.computeIfAbsent(entityKey.getType(), entityKeyType -> new HashMap<>()).put(entityKey.getKey(), tsValue);
            }
        }
        return entityData;
    }

    static String convertValue(@Nullable Object value) {
        if (value != null) {
            String strVal = value.toString();
            // check number
            if (NumberUtils.isParsable(strVal)) {
                if (strVal.startsWith("0") && !strVal.startsWith("0.")) {
                    return strVal;
                }
                try {
                    long longVal = Long.parseLong(strVal);
                    return Long.toString(longVal);
                } catch (NumberFormatException ignored) {
                }
                try {
                    double dblVal = Double.parseDouble(strVal);
                    String doubleAsString = Double.toString(dblVal);
                    if (!Double.isInfinite(dblVal) && isSimpleDouble(doubleAsString)) {
                        return doubleAsString;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return strVal;
        } else {
            return "";
        }
    }

    private static boolean isSimpleDouble(@NotNull String valueAsString) {
        return valueAsString.contains(".") && !valueAsString.contains("E") && !valueAsString.contains("e");
    }


}
