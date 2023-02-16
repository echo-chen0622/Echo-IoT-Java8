package org.echoiot.server.dao;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.echoiot.server.dao.util.SqlTsOrTsLatestAnyDao;
import org.echoiot.server.dao.util.TbAutoConfiguration;

@Configuration
@TbAutoConfiguration
@EnableJpaRepositories({"org.thingsboard.server.dao.sqlts.dictionary"})
@EntityScan({"org.thingsboard.server.dao.model.sqlts.dictionary"})
@EnableTransactionManagement
@SqlTsOrTsLatestAnyDao
public class SqlTimeseriesDaoConfig {

}
