package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class TestProperties {

    private static final String HTTPS_URL = "https://localhost";

    private static final String WSS_URL = "wss://localhost";

    private static final ContainerTestSuite instance = ContainerTestSuite.getInstance();

    private static Properties properties;

    public static String getBaseUrl() {
        if (instance.isActive()) {
            return HTTPS_URL;
        }
        return getProperties().getProperty("tb.baseUrl");
    }

    public static String getBaseUiUrl() {
        if (instance.isActive()) {
            return "https://host.docker.internal";
        }
        return getProperties().getProperty("tb.baseUiUrl");
    }

    public static String getWebSocketUrl() {
        if (instance.isActive()) {
            return WSS_URL;
        }
        return getProperties().getProperty("tb.wsUrl");
    }

    private static Properties getProperties() {
        if (properties == null) {
            try (InputStream input = TestProperties.class.getClassLoader().getResourceAsStream("config.properties")) {
                properties = new Properties();
                properties.load(input);
            } catch (IOException ex) {
                log.error("Exception while reading test properties " + ex.getMessage());
            }
        }
        return properties;
    }

}
