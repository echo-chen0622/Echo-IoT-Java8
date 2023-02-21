package org.echoiot.server.service.sync.ie;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.ExportableEntity;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.sync.ThrowingRunnable;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.common.data.sync.ie.EntityImportResult;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.relation.RelationService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.apiusage.RateLimitService;
import org.echoiot.server.service.entitiy.TbNotificationEntityService;
import org.echoiot.server.service.sync.ie.exporting.EntityExportService;
import org.echoiot.server.service.sync.ie.exporting.impl.BaseEntityExportService;
import org.echoiot.server.service.sync.ie.exporting.impl.DefaultEntityExportService;
import org.echoiot.server.service.sync.ie.importing.EntityImportService;
import org.echoiot.server.service.sync.ie.importing.impl.MissingEntityException;
import org.echoiot.server.service.sync.vc.LoadEntityException;
import org.echoiot.server.service.sync.vc.data.EntitiesExportCtx;
import org.echoiot.server.service.sync.vc.data.EntitiesImportCtx;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultEntitiesExportImportService implements EntitiesExportImportService {

    private final Map<EntityType, EntityExportService<?, ?, ?>> exportServices = new HashMap<>();
    private final Map<EntityType, EntityImportService<?, ?, ?>> importServices = new HashMap<>();

    @NotNull
    private final RelationService relationService;
    @NotNull
    private final RateLimitService rateLimitService;
    @NotNull
    private final TbNotificationEntityService entityNotificationService;

    protected static final List<EntityType> SUPPORTED_ENTITY_TYPES = List.of(
            EntityType.CUSTOMER, EntityType.ASSET_PROFILE, EntityType.ASSET, EntityType.RULE_CHAIN,
            EntityType.DASHBOARD, EntityType.DEVICE_PROFILE, EntityType.DEVICE,
            EntityType.ENTITY_VIEW, EntityType.WIDGETS_BUNDLE
    );


    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(@NotNull EntitiesExportCtx<?> ctx, @NotNull I entityId) throws EchoiotException {
        if (!rateLimitService.checkEntityExportLimit(ctx.getTenantId())) {
            throw new EchoiotException("Rate limit for entities export is exceeded", EchoiotErrorCode.TOO_MANY_REQUESTS);
        }

        EntityType entityType = entityId.getEntityType();
        @NotNull EntityExportService<I, E, EntityExportData<E>> exportService = getExportService(entityType);

        return exportService.getExportData(ctx, entityId);
    }

    @NotNull
    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(@NotNull EntitiesImportCtx ctx, @NotNull EntityExportData<E> exportData) throws EchoiotException {
        if (!rateLimitService.checkEntityImportLimit(ctx.getTenantId())) {
            throw new EchoiotException("Rate limit for entities import is exceeded", EchoiotErrorCode.TOO_MANY_REQUESTS);
        }
        if (exportData.getEntity() == null || exportData.getEntity().getId() == null) {
            throw new DataValidationException("Invalid entity data");
        }

        EntityType entityType = exportData.getEntityType();
        @NotNull EntityImportService<I, E, EntityExportData<E>> importService = getImportService(entityType);

        EntityImportResult<E> importResult = importService.importEntity(ctx, exportData);
        ctx.putInternalId(exportData.getExternalId(), importResult.getSavedEntity().getId());

        ctx.addReferenceCallback(exportData.getExternalId(), importResult.getSaveReferencesCallback());
        ctx.addEventCallback(importResult.getSendEventsCallback());
        return importResult;
    }

    @Override
    public void saveReferencesAndRelations(@NotNull EntitiesImportCtx ctx) throws EchoiotException {
        for (@NotNull Map.Entry<EntityId, ThrowingRunnable> callbackEntry : ctx.getReferenceCallbacks().entrySet()) {
            EntityId externalId = callbackEntry.getKey();
            ThrowingRunnable saveReferencesCallback = callbackEntry.getValue();
            try {
                saveReferencesCallback.run();
            } catch (MissingEntityException e) {
                throw new LoadEntityException(externalId, e);
            }
        }

        relationService.saveRelations(ctx.getTenantId(), new ArrayList<>(ctx.getRelations()));

        for (EntityRelation relation : ctx.getRelations()) {
            entityNotificationService.notifyRelation(ctx.getTenantId(), null,
                                                     relation, ctx.getUser(), ActionType.RELATION_ADD_OR_UPDATE, relation);
        }
    }


    @Override
    public Comparator<EntityType> getEntityTypeComparatorForImport() {
        return Comparator.comparing(SUPPORTED_ENTITY_TYPES::indexOf);
    }


    @NotNull
    @SuppressWarnings("unchecked")
    private <I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> EntityExportService<I, E, D> getExportService(EntityType entityType) {
        EntityExportService<?, ?, ?> exportService = exportServices.get(entityType);
        if (exportService == null) {
            throw new IllegalArgumentException("Export for entity type " + entityType + " is not supported");
        }
        return (EntityExportService<I, E, D>) exportService;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private <I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> EntityImportService<I, E, D> getImportService(EntityType entityType) {
        EntityImportService<?, ?, ?> importService = importServices.get(entityType);
        if (importService == null) {
            throw new IllegalArgumentException("Import for entity type " + entityType + " is not supported");
        }
        return (EntityImportService<I, E, D>) importService;
    }

    @Resource
    private void setExportServices(DefaultEntityExportService<?, ?, ?> defaultExportService,
                                   @NotNull Collection<BaseEntityExportService<?, ?, ?>> exportServices) {
        exportServices.stream()
                .sorted(Comparator.comparing(exportService -> exportService.getSupportedEntityTypes().size(), Comparator.reverseOrder()))
                .forEach(exportService -> {
                    exportService.getSupportedEntityTypes().forEach(entityType -> {
                        this.exportServices.put(entityType, exportService);
                    });
                });
        SUPPORTED_ENTITY_TYPES.forEach(entityType -> {
            this.exportServices.putIfAbsent(entityType, defaultExportService);
        });
    }

    @Resource
    private void setImportServices(@NotNull Collection<EntityImportService<?, ?, ?>> importServices) {
        importServices.forEach(entityImportService -> {
            this.importServices.put(entityImportService.getEntityType(), entityImportService);
        });
    }

}
