package org.echoiot.server.service.sync.vc.data;

import lombok.Data;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.common.data.sync.ie.EntityImportSettings;

@Data
public class ReimportTask {

    private final EntityExportData data;
    private final EntityImportSettings settings;

}
