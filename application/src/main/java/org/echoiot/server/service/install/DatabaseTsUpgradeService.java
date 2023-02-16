package org.echoiot.server.service.install;

public interface DatabaseTsUpgradeService {

    void upgradeDatabase(String fromVersion) throws Exception;

}
