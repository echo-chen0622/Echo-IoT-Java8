package org.echoiot.server.dao;

import org.echoiot.server.dao.util.TbAutoConfiguration;
import org.echoiot.server.dao.util.TimescaleDBTsDao;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@TbAutoConfiguration
@ComponentScan({"org.echoiot.server.dao.sqlts.timescale"})
@EnableJpaRepositories({"org.echoiot.server.dao.sqlts.timescale", "org.echoiot.server.dao.sqlts.insert.timescale"})
@EntityScan({"org.echoiot.server.dao.model.sqlts.timescale"})
@EnableTransactionManagement
@TimescaleDBTsDao
public class TimescaleDaoConfig {

}
