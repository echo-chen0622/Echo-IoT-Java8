package org.thingsboard.server.service.install;

public interface DatabaseTsUpgradeService {

    void upgradeDatabase(String fromVersion) throws Exception;

}
