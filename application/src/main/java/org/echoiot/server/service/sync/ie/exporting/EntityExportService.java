package org.echoiot.server.service.sync.ie.exporting;

import org.echoiot.server.common.data.ExportableEntity;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.service.sync.vc.data.EntitiesExportCtx;

public interface EntityExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> {

    D getExportData(EntitiesExportCtx<?> ctx, I entityId) throws EchoiotException;

}
