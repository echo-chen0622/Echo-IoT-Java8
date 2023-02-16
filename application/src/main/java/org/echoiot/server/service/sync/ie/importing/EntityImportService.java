package org.echoiot.server.service.sync.ie.importing;

import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.ExportableEntity;
import org.echoiot.server.common.data.exception.ThingsboardException;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.common.data.sync.ie.EntityImportResult;
import org.echoiot.server.service.sync.vc.data.EntitiesImportCtx;

public interface EntityImportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> {

    EntityImportResult<E> importEntity(EntitiesImportCtx ctx, D exportData) throws ThingsboardException;

    EntityType getEntityType();

}
