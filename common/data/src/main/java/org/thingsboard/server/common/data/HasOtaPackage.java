package org.thingsboard.server.common.data;

import org.thingsboard.server.common.data.id.OtaPackageId;

public interface HasOtaPackage {

    OtaPackageId getFirmwareId();

    OtaPackageId getSoftwareId();
}
