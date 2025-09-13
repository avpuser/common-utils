package com.avpuser.utils;

import com.google.api.client.util.DateTime;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static final ZoneId MOSCOW_ZONE = ZoneId.of("Europe/Moscow");

    public static String formatDateTime(Instant instant) {
        return formatDateTime(instant, "yyyy_MM_dd");
    }

    public static String formatDateTime(Instant instant, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        LocalDateTime localDateTime = instant.atZone(DateUtils.MOSCOW_ZONE).toLocalDateTime();

        return localDateTime.format(formatter);
    }

    public static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        return hours > 0 ? DurationFormatUtils.formatDuration(duration.toMillis(), "HH:mm:ss")
                : DurationFormatUtils.formatDuration(duration.toMillis(), "mm:ss");
    }

    public static String formatDateTime(DateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");


        String rfc3339DateTime = dateTime.toStringRfc3339();
        LocalDateTime localDateTime = LocalDateTime.parse(rfc3339DateTime, DateTimeFormatter.ISO_DATE_TIME);

        return localDateTime.format(formatter);
    }

    public static Instant convertToInstant(DateTime dateTime) {
        return Instant.ofEpochMilli(dateTime.getValue());
    }

    public static Instant parse(String s) {
        LocalDate localDate = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);

        return localDate.atStartOfDay(ZoneId.of("UTC")).toInstant();
    }

    public static LocalDate toLocalDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(DateUtils.MOSCOW_ZONE).toLocalDate();
    }

}
