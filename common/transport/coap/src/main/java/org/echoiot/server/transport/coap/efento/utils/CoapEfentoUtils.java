package org.echoiot.server.transport.coap.efento.utils;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class CoapEfentoUtils {

    @NotNull
    public static String convertByteArrayToString(@NotNull byte[] a) {
        @NotNull StringBuilder out = new StringBuilder();
        for (byte b : a) {
            out.append(String.format("%02X", b));
        }
        return out.toString();
    }

    public static String convertTimestampToUtcString(long timestampInMillis) {
        @NotNull String dateFormat = "yyyy-MM-dd HH:mm:ss";
        @NotNull String utcZone = "UTC";
        @NotNull SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(utcZone));
        return String.format("%s UTC", simpleDateFormat.format(new Date(timestampInMillis)));
    }

    @NotNull
    public static JsonObject setDefaultMeasurements(String serialNumber, boolean batteryStatus, long measurementPeriod, long nextTransmissionAtMillis, long signal, long startTimestampMillis) {
        @NotNull JsonObject values = new JsonObject();
        values.addProperty("serial", serialNumber);
        values.addProperty("battery", batteryStatus ? "ok" : "low");
        values.addProperty("measured_at", convertTimestampToUtcString(startTimestampMillis));
        values.addProperty("next_transmission_at", convertTimestampToUtcString(nextTransmissionAtMillis));
        values.addProperty("signal", signal);
        values.addProperty("measurement_interval", measurementPeriod);
        return values;
    }

}
