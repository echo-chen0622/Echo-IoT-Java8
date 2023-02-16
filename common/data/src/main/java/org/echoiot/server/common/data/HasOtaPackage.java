package org.echoiot.server.common.data;

import org.echoiot.server.common.data.id.OtaPackageId;

public interface HasOtaPackage {

    OtaPackageId getFirmwareId();

    OtaPackageId getSoftwareId();
}
