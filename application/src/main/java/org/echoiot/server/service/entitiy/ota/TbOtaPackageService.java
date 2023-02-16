package org.echoiot.server.service.entitiy.ota;

import org.echoiot.server.common.data.OtaPackageInfo;
import org.echoiot.server.common.data.SaveOtaPackageInfoRequest;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.ota.ChecksumAlgorithm;

public interface TbOtaPackageService {

    OtaPackageInfo save(SaveOtaPackageInfoRequest saveOtaPackageInfoRequest, User user) throws EchoiotException;

    OtaPackageInfo saveOtaPackageData(OtaPackageInfo otaPackageInfo, String checksum, ChecksumAlgorithm checksumAlgorithm,
                                      byte[] data, String filename, String contentType, User user) throws EchoiotException;

    void delete(OtaPackageInfo otaPackageInfo, User user) throws EchoiotException;

}
