package org.echoiot.server.dao;

import org.echoiot.server.dao.util.TbAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Valerii Sosliuk
 */
@Configuration
@TbAutoConfiguration
@ComponentScan({"org.echoiot.server.dao.sql", "org.echoiot.server.dao.attributes", "org.echoiot.server.dao.cache", "org.echoiot.server.cache"})
@EnableJpaRepositories("org.echoiot.server.dao.sql")
@EntityScan("org.echoiot.server.dao.model.sql")
@EnableTransactionManagement
public class JpaDaoConfig {

}
