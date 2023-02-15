package org.thingsboard.server.transport.coap.efento.utils;

import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class CoapEfentoUtils {

    public static String convertByteArrayToString(byte[] a) {
        StringBuilder out = new StringBuilder();
        for (byte b : a) {
            out.append(String.format("%02X", b));
        }
        return out.toString();
    }

    public static String convertTimestampToUtcString(long timestampInMillis) {
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        String utcZone = "UTC";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(utcZone));
        return String.format("%s UTC", simpleDateFormat.format(new Date(timestampInMillis)));
    }

    public static JsonObject setDefaultMeasurements(String serialNumber, boolean batteryStatus, long measurementPeriod, long nextTransmissionAtMillis, long signal, long startTimestampMillis) {
        JsonObject values = new JsonObject();
        values.addProperty("serial", serialNumber);
        values.addProperty("battery", batteryStatus ? "ok" : "low");
        values.addProperty("measured_at", convertTimestampToUtcString(startTimestampMillis));
        values.addProperty("next_transmission_at", convertTimestampToUtcString(nextTransmissionAtMillis));
        values.addProperty("signal", signal);
        values.addProperty("measurement_interval", measurementPeriod);
        return values;
    }

}
