package org.echoiot.server.service.entitiy.ota;

import org.echoiot.server.common.data.OtaPackageInfo;
import org.echoiot.server.common.data.SaveOtaPackageInfoRequest;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.exception.ThingsboardException;
import org.echoiot.server.common.data.ota.ChecksumAlgorithm;

public interface TbOtaPackageService {

    OtaPackageInfo save(SaveOtaPackageInfoRequest saveOtaPackageInfoRequest, User user) throws ThingsboardException;

    OtaPackageInfo saveOtaPackageData(OtaPackageInfo otaPackageInfo, String checksum, ChecksumAlgorithm checksumAlgorithm,
                                      byte[] data, String filename, String contentType, User user) throws ThingsboardException;

    void delete(OtaPackageInfo otaPackageInfo, User user) throws ThingsboardException;

}
