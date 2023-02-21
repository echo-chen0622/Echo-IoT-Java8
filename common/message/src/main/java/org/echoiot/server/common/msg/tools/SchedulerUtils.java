package org.echoiot.server.common.msg.tools;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoUnit.MONTHS;

public class SchedulerUtils {

    private static final ConcurrentMap<String, ZoneId> tzMap = new ConcurrentHashMap<>();

    @NotNull
    public static ZoneId getZoneId(@Nullable String tz) {
        return tzMap.computeIfAbsent(tz == null || tz.isEmpty() ? "UTC" : tz, ZoneId::of);
    }

    public static long getStartOfCurrentHour() {
        return getStartOfCurrentHour(UTC);
    }

    public static long getStartOfCurrentHour(@NotNull ZoneId zoneId) {
        return LocalDateTime.now(UTC).atZone(zoneId).truncatedTo(ChronoUnit.HOURS).toInstant().toEpochMilli();
    }

    public static long getStartOfCurrentMonth() {
        return getStartOfCurrentMonth(UTC);
    }

    public static long getStartOfCurrentMonth(@NotNull ZoneId zoneId) {
        return LocalDate.now(UTC).withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

    public static long getStartOfNextMonth() {
        return getStartOfNextMonth(UTC);
    }

    public static long getStartOfNextMonth(@NotNull ZoneId zoneId) {
        return LocalDate.now(UTC).with(TemporalAdjusters.firstDayOfNextMonth()).atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

    public static long getStartOfNextNextMonth() {
        return getStartOfNextNextMonth(UTC);
    }

    public static long getStartOfNextNextMonth(@NotNull ZoneId zoneId) {
        return LocalDate.now(UTC).with(firstDayOfNextNextMonth()).atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

    @NotNull
    public static TemporalAdjuster firstDayOfNextNextMonth() {
        return (temporal) -> temporal.with(DAY_OF_MONTH, 1).plus(2, MONTHS);
    }

}
