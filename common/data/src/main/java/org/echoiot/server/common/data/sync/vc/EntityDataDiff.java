package org.echoiot.server.common.data.sync.vc;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.echoiot.server.common.data.sync.ie.EntityExportData;

@Data
@AllArgsConstructor
public class EntityDataDiff {
    private EntityExportData<?> currentVersion;
    private EntityExportData<?> otherVersion;
}
