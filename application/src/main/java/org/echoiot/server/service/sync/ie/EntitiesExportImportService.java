package org.echoiot.server.service.sync.ie;

import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.ExportableEntity;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.common.data.sync.ie.EntityImportResult;
import org.echoiot.server.service.sync.vc.data.EntitiesExportCtx;
import org.echoiot.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.Comparator;

public interface EntitiesExportImportService {

    <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(EntitiesExportCtx<?> ctx, I entityId) throws EchoiotException;

    <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(EntitiesImportCtx ctx, EntityExportData<E> exportData) throws EchoiotException;


    void saveReferencesAndRelations(EntitiesImportCtx ctx) throws EchoiotException;

    Comparator<EntityType> getEntityTypeComparatorForImport();

}
