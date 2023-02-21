package org.echoiot.server.service.sync.ie.exporting.impl;

import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.ExportableEntity;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.common.data.sync.ie.AttributeExportData;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.relation.RelationDao;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.sync.ie.exporting.EntityExportService;
import org.echoiot.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.echoiot.server.service.sync.vc.data.EntitiesExportCtx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@Primary
public class DefaultEntityExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> implements EntityExportService<I, E, D> {

    @Resource
    @Lazy
    protected ExportableEntitiesService exportableEntitiesService;
    @Resource
    private RelationDao relationDao;
    @Resource
    private AttributesService attributesService;

    @NotNull
    @Override
    public final D getExportData(@NotNull EntitiesExportCtx<?> ctx, @NotNull I entityId) throws EchoiotException {
        D exportData = newExportData();

        E entity = exportableEntitiesService.findEntityByTenantIdAndId(ctx.getTenantId(), entityId);
        if (entity == null) {
            throw new IllegalArgumentException(entityId.getEntityType() + " [" + entityId.getId() + "] not found");
        }

        exportData.setEntity(entity);
        exportData.setEntityType(entityId.getEntityType());
        setAdditionalExportData(ctx, entity, exportData);

        var externalId = entity.getExternalId() != null ? entity.getExternalId() : entity.getId();
        ctx.putExternalId(entityId, externalId);
        entity.setId(externalId);
        entity.setTenantId(null);

        return exportData;
    }

    protected void setAdditionalExportData(@NotNull EntitiesExportCtx<?> ctx, @NotNull E entity, @NotNull D exportData) throws EchoiotException {
        var exportSettings = ctx.getSettings();
        if (exportSettings.isExportRelations()) {
            @NotNull List<EntityRelation> relations = exportRelations(ctx, entity);
            relations.forEach(relation -> {
                relation.setFrom(getExternalIdOrElseInternal(ctx, relation.getFrom()));
                relation.setTo(getExternalIdOrElseInternal(ctx, relation.getTo()));
            });
            exportData.setRelations(relations);
        }
        if (exportSettings.isExportAttributes()) {
            @NotNull Map<String, List<AttributeExportData>> attributes = exportAttributes(ctx, entity);
            exportData.setAttributes(attributes);
        }
    }

    @NotNull
    private List<EntityRelation> exportRelations(@NotNull EntitiesExportCtx<?> ctx, @NotNull E entity) throws EchoiotException {
        @NotNull List<EntityRelation> relations = new ArrayList<>();

        List<EntityRelation> inboundRelations = relationDao.findAllByTo(ctx.getTenantId(), entity.getId(), RelationTypeGroup.COMMON);
        relations.addAll(inboundRelations);

        List<EntityRelation> outboundRelations = relationDao.findAllByFrom(ctx.getTenantId(), entity.getId(), RelationTypeGroup.COMMON);
        relations.addAll(outboundRelations);
        return relations;
    }

    @NotNull
    private Map<String, List<AttributeExportData>> exportAttributes(@NotNull EntitiesExportCtx<?> ctx, @NotNull E entity) throws EchoiotException {
        List<String> scopes;
        if (entity.getId().getEntityType() == EntityType.DEVICE) {
            scopes = List.of(DataConstants.SERVER_SCOPE, DataConstants.SHARED_SCOPE);
        } else {
            scopes = Collections.singletonList(DataConstants.SERVER_SCOPE);
        }
        @NotNull Map<String, List<AttributeExportData>> attributes = new LinkedHashMap<>();
        scopes.forEach(scope -> {
            try {
                attributes.put(scope, attributesService.findAll(ctx.getTenantId(), entity.getId(), scope).get().stream()
                        .map(attribute -> {
                            @NotNull AttributeExportData attributeExportData = new AttributeExportData();
                            attributeExportData.setKey(attribute.getKey());
                            attributeExportData.setLastUpdateTs(attribute.getLastUpdateTs());
                            attributeExportData.setStrValue(attribute.getStrValue().orElse(null));
                            attributeExportData.setDoubleValue(attribute.getDoubleValue().orElse(null));
                            attributeExportData.setLongValue(attribute.getLongValue().orElse(null));
                            attributeExportData.setBooleanValue(attribute.getBooleanValue().orElse(null));
                            attributeExportData.setJsonValue(attribute.getJsonValue().orElse(null));
                            return attributeExportData;
                        })
                        .collect(Collectors.toList()));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        return attributes;
    }

    @Nullable
    protected <ID extends EntityId> ID getExternalIdOrElseInternal(@NotNull EntitiesExportCtx<?> ctx, @Nullable ID internalId) {
        if (internalId == null || internalId.isNullUid()) return internalId;
        var result = ctx.getExternalId(internalId);
        if (result == null) {
            result = Optional.ofNullable(exportableEntitiesService.getExternalIdByInternal(internalId))
                    .orElse(internalId);
            ctx.putExternalId(internalId, result);
        }
        return result;
    }

    protected UUID getExternalIdOrElseInternalByUuid(@NotNull EntitiesExportCtx<?> ctx, UUID internalUuid) {
        for (@NotNull EntityType entityType : EntityType.values()) {
            EntityId internalId;
            try {
                internalId = EntityIdFactory.getByTypeAndUuid(entityType, internalUuid);
            } catch (Exception e) {
                continue;
            }
            EntityId externalId = ctx.getExternalId(internalId);
            if (externalId != null) {
                return externalId.getId();
            }
        }
        for (@NotNull EntityType entityType : EntityType.values()) {
            EntityId internalId;
            try {
                internalId = EntityIdFactory.getByTypeAndUuid(entityType, internalUuid);
            } catch (Exception e) {
                continue;
            }
            EntityId externalId = exportableEntitiesService.getExternalIdByInternal(internalId);
            if (externalId != null) {
                ctx.putExternalId(internalId, externalId);
                return externalId.getId();
            }
        }
        return internalUuid;
    }

    protected D newExportData() {
        return (D) new EntityExportData<E>();
    }

}
