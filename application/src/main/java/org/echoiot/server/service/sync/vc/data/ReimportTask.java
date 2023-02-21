package org.echoiot.server.service.sync.vc.data;

import lombok.Data;
import org.echoiot.server.common.data.sync.ie.EntityExportData;
import org.echoiot.server.common.data.sync.ie.EntityImportSettings;
import org.jetbrains.annotations.NotNull;

@Data
public class ReimportTask {

    @NotNull
    private final EntityExportData data;
    @NotNull
    private final EntityImportSettings settings;

}
