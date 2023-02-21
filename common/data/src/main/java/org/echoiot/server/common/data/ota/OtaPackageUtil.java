package org.echoiot.server.common.data.ota;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.HasOtaPackage;
import org.echoiot.server.common.data.id.OtaPackageId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class OtaPackageUtil {

    @NotNull
    public static final List<String> ALL_FW_ATTRIBUTE_KEYS;

    @NotNull
    public static final List<String> ALL_SW_ATTRIBUTE_KEYS;

    static {
        ALL_FW_ATTRIBUTE_KEYS = new ArrayList<>();
        for (@NotNull OtaPackageKey key : OtaPackageKey.values()) {
            ALL_FW_ATTRIBUTE_KEYS.add(getAttributeKey(OtaPackageType.FIRMWARE, key));

        }

        ALL_SW_ATTRIBUTE_KEYS = new ArrayList<>();
        for (@NotNull OtaPackageKey key : OtaPackageKey.values()) {
            ALL_SW_ATTRIBUTE_KEYS.add(getAttributeKey(OtaPackageType.SOFTWARE, key));

        }
    }

    public static List<String> getAttributeKeys(@NotNull OtaPackageType firmwareType) {
        switch (firmwareType) {
            case FIRMWARE:
                return ALL_FW_ATTRIBUTE_KEYS;
            case SOFTWARE:
                return ALL_SW_ATTRIBUTE_KEYS;
        }
        return Collections.emptyList();
    }

    @NotNull
    public static String getAttributeKey(@NotNull OtaPackageType type, @NotNull OtaPackageKey key) {
        return type.getKeyPrefix() + "_" + key.getValue();
    }

    @NotNull
    public static String getTargetTelemetryKey(@NotNull OtaPackageType type, @NotNull OtaPackageKey key) {
        return getTelemetryKey("target_", type, key);
    }

    @NotNull
    public static String getCurrentTelemetryKey(@NotNull OtaPackageType type, @NotNull OtaPackageKey key) {
        return getTelemetryKey("current_", type, key);
    }

    @NotNull
    private static String getTelemetryKey(String prefix, @NotNull OtaPackageType type, @NotNull OtaPackageKey key) {
        return prefix + type.getKeyPrefix() + "_" + key.getValue();
    }

    @NotNull
    public static String getTelemetryKey(@NotNull OtaPackageType type, @NotNull OtaPackageKey key) {
        return type.getKeyPrefix() + "_" + key.getValue();
    }

    @Nullable
    public static OtaPackageId getOtaPackageId(@NotNull HasOtaPackage entity, @NotNull OtaPackageType type) {
        switch (type) {
            case FIRMWARE:
                return entity.getFirmwareId();
            case SOFTWARE:
                return entity.getSoftwareId();
            default:
                log.warn("Unsupported ota package type: [{}]", type);
                return null;
        }
    }

    public static <T> T getByOtaPackageType(@NotNull Supplier<T> firmwareSupplier, @NotNull Supplier<T> softwareSupplier, @NotNull OtaPackageType type) {
        switch (type) {
            case FIRMWARE:
                return firmwareSupplier.get();
            case SOFTWARE:
                return softwareSupplier.get();
            default:
                throw new RuntimeException("Unsupported OtaPackage type: " + type);
        }
    }
}
