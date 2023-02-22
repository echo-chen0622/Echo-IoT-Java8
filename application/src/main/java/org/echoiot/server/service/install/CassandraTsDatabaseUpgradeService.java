package org.echoiot.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.util.NoSqlTsDao;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@NoSqlTsDao
@Profile("install")
@Slf4j
public class CassandraTsDatabaseUpgradeService extends AbstractCassandraDatabaseUpgradeService implements DatabaseTsUpgradeService {

    @Override
    public void upgradeDatabase(String fromVersion) throws Exception {
        switch (fromVersion) {
            //这里要注意，每个case需要加break
            case "1.0.0":
                break;
            case "1.0.1":
                break;
            default:
                throw new RuntimeException("无法升级 Cassandra 数据库, 升级源版本 fromVersion: " + fromVersion);
        }
    }

}
