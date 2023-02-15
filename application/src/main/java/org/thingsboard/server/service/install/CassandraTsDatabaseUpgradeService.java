package org.thingsboard.server.service.install;

import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.util.NoSqlTsDao;

@Service
@NoSqlTsDao
@Profile("install")
@Slf4j
public class CassandraTsDatabaseUpgradeService extends AbstractCassandraDatabaseUpgradeService implements DatabaseTsUpgradeService {

    @Override
    public void upgradeDatabase(String fromVersion) throws Exception {
        switch (fromVersion) {
            case "2.4.3":
                log.info("Updating schema ...");
                String updateTsKvTableStmt = "alter table ts_kv_cf add json_v text";
                String updateTsKvLatestTableStmt = "alter table ts_kv_latest_cf add json_v text";

                try {
                    log.info("Updating ts ...");
                    cluster.getSession().execute(updateTsKvTableStmt);
                    Thread.sleep(2500);
                    log.info("Ts updated.");
                    log.info("Updating ts latest ...");
                    cluster.getSession().execute(updateTsKvLatestTableStmt);
                    Thread.sleep(2500);
                    log.info("Ts latest updated.");
                } catch (InvalidQueryException e) {
                }
                log.info("Schema updated.");
                break;
            case "2.5.0":
            case "3.1.1":
            case "3.2.1":
            case "3.2.2":
                break;
            default:
                throw new RuntimeException("Unable to upgrade Cassandra database, unsupported fromVersion: " + fromVersion);
        }
    }

}
