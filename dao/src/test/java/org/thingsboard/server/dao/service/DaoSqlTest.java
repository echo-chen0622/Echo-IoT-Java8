package org.thingsboard.server.dao.service;

import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@TestPropertySource(locations = {"classpath:application-test.properties", "classpath:sql-test.properties"})
public @interface DaoSqlTest {
}
