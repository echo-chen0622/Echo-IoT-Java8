package org.echoiot.server.cache;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CaffeineCacheDefaultConfigurationTest.class, loader = SpringBootContextLoader.class)
@ComponentScan({"org.echoiot.server.cache"})
@EnableConfigurationProperties
@Slf4j
public class CaffeineCacheDefaultConfigurationTest {

    @Resource
    CacheSpecsMap cacheSpecsMap;

    @Test
    public void verifyTransactionAwareCacheManagerProxy() {
        assertThat(cacheSpecsMap.getSpecs()).as("specs").isNotNull();
        cacheSpecsMap.getSpecs().forEach((name, cacheSpecs)->assertThat(cacheSpecs).as("cache %s specs", name).isNotNull());

        @NotNull SoftAssertions softly = new SoftAssertions();
        cacheSpecsMap.getSpecs().forEach((name, cacheSpecs)->{
            softly.assertThat(name).as("cache name").isNotEmpty();
            softly.assertThat(cacheSpecs.getTimeToLiveInMinutes()).as("cache %s time to live", name).isGreaterThan(0);
            softly.assertThat(cacheSpecs.getMaxSize()).as("cache %s max size", name).isGreaterThan(0);
        });
        softly.assertAll();
    }

}
