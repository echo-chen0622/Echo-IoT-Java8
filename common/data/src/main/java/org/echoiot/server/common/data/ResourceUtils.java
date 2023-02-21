package org.echoiot.server.common.data;

import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

@Slf4j
public class ResourceUtils {

    public static final String CLASSPATH_URL_PREFIX = "classpath:";

    public static boolean resourceExists(@NotNull Object classLoaderSource, String filePath) {
        return resourceExists(classLoaderSource.getClass().getClassLoader(), filePath);
    }

    public static boolean resourceExists(@NotNull ClassLoader classLoader, String filePath) {
        boolean classPathResource = false;
        String path = filePath;
        if (path.startsWith(CLASSPATH_URL_PREFIX)) {
            path = path.substring(CLASSPATH_URL_PREFIX.length());
            classPathResource = true;
        }
        if (!classPathResource) {
            @NotNull File resourceFile = new File(path);
            if (resourceFile.exists()) {
                return true;
            }
        }
        @Nullable InputStream classPathStream = classLoader.getResourceAsStream(path);
        if (classPathStream != null) {
            return true;
        } else {
            try {
                @NotNull URL url = Resources.getResource(path);
                if (url != null) {
                    return true;
                }
            } catch (IllegalArgumentException e) {}
        }
        return false;
    }

    @NotNull
    public static InputStream getInputStream(@NotNull Object classLoaderSource, String filePath) {
        return getInputStream(classLoaderSource.getClass().getClassLoader(), filePath);
    }

    @NotNull
    public static InputStream getInputStream(@NotNull ClassLoader classLoader, String filePath) {
        boolean classPathResource = false;
        String path = filePath;
        if (path.startsWith(CLASSPATH_URL_PREFIX)) {
            path = path.substring(CLASSPATH_URL_PREFIX.length());
            classPathResource = true;
        }
        try {
            if (!classPathResource) {
                @NotNull File resourceFile = new File(path);
                if (resourceFile.exists()) {
                    log.info("Reading resource data from file {}", filePath);
                    return new FileInputStream(resourceFile);
                }
            }
            @Nullable InputStream classPathStream = classLoader.getResourceAsStream(path);
            if (classPathStream != null) {
                log.info("Reading resource data from class path {}", filePath);
                return classPathStream;
            } else {
                @NotNull URL url = Resources.getResource(path);
                if (url != null) {
                    @NotNull URI uri = url.toURI();
                    log.info("Reading resource data from URI {}", filePath);
                    return new FileInputStream(new File(uri));
                }
            }
        } catch (Exception e) {
            if (e instanceof NullPointerException) {
                log.warn("Unable to find resource: " + filePath);
            } else {
                log.warn("Unable to find resource: " + filePath, e);
            }
        }
        throw new RuntimeException("Unable to find resource: " + filePath);
    }

    public static String getUri(@NotNull Object classLoaderSource, @NotNull String filePath) {
        return getUri(classLoaderSource.getClass().getClassLoader(), filePath);
    }

    public static String getUri(@NotNull ClassLoader classLoader, @NotNull String filePath) {
        try {
            @NotNull File resourceFile = new File(filePath);
            if (resourceFile.exists()) {
                log.info("Reading resource data from file {}", filePath);
                return resourceFile.getAbsolutePath();
            } else {
                @Nullable URL url = classLoader.getResource(filePath);
                return url.toURI().toString();
            }
        } catch (Exception e) {
            if (e instanceof NullPointerException) {
                log.warn("Unable to find resource: " + filePath);
            } else {
                log.warn("Unable to find resource: " + filePath, e);
            }
            throw new RuntimeException("Unable to find resource: " + filePath);
        }
    }
}
