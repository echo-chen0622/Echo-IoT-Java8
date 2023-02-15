package org.thingsboard.server.dao;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.thingsboard.server.dao.util.TbAutoConfiguration;

/**
 * @author Valerii Sosliuk
 */
@Configuration
@TbAutoConfiguration
@ComponentScan({"org.thingsboard.server.dao.sql", "org.thingsboard.server.dao.attributes", "org.thingsboard.server.dao.cache", "org.thingsboard.server.cache"})
@EnableJpaRepositories("org.thingsboard.server.dao.sql")
@EntityScan("org.thingsboard.server.dao.model.sql")
@EnableTransactionManagement
public class JpaDaoConfig {

}
