package org.echoiot.script.api.tbel;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class TbDate extends Date {

    private static final DateTimeFormatter isoDateFormatter = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd[[ ]['T']HH:mm[:ss[.SSS]][ ][XXX][Z][z][VV][O]]").withZone(ZoneId.systemDefault());

    private static final DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public TbDate() {
        super();
    }

    public TbDate(@NotNull String s) {
        super(parse(s));
    }

    public TbDate(long date) {
        super(date);
    }

    public TbDate(int year, int month, int date) {
        this(year, month, date, 0, 0, 0);
    }

    public TbDate(int year, int month, int date, int hrs, int min) {
        this(year, month, date, hrs, min, 0);
    }

    public TbDate(int year, int month, int date,
                  int hrs, int min, int second) {
        super(new GregorianCalendar(year, month, date, hrs, min, second).getTimeInMillis());
    }

    @NotNull
    public String toDateString() {
        DateFormat formatter = DateFormat.getDateInstance();
        return formatter.format(this);
    }

    @NotNull
    public String toTimeString() {
        DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
        return formatter.format(this);
    }

    @NotNull
    public String toISOString() {
        return isoDateFormat.format(this);
    }

    @NotNull
    public String toLocaleString(@NotNull String locale) {
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.forLanguageTag(locale));
        return formatter.format(this);
    }

    @NotNull
    public String toLocaleString(@NotNull String locale, String tz) {
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.forLanguageTag(locale));
        formatter.setTimeZone(TimeZone.getTimeZone(tz));
        return formatter.format(this);
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    public static long parse(String value, @NotNull String format) {
        try {
            @NotNull DateFormat dateFormat = new SimpleDateFormat(format);
            return dateFormat.parse(value).getTime();
        } catch (Exception e) {
            return -1;
        }
    }

    public static long parse(@NotNull String value) {
        try {
            TemporalAccessor accessor = isoDateFormatter.parseBest(value,
                    ZonedDateTime::from,
                    LocalDateTime::from,
                    LocalDate::from);
            Instant instant = Instant.from(accessor);
            return Instant.EPOCH.until(instant, ChronoUnit.MILLIS);
        } catch (Exception e) {
            try {
                return Date.parse(value);
            } catch (IllegalArgumentException e2) {
                return -1;
            }
        }
    }

    public static long UTC(int year, int month, int date,
                           int hrs, int min, int sec) {
        return Date.UTC(year - 1900, month, date, hrs, min, sec);
    }

}
