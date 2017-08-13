package org.openhab.binding.intellihouse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {

    private static final String ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    public static String toString(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(ISO8601_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public static Date toDate(String string) throws IllegalArgumentException {
        if (string == null || string.isEmpty()) {
            return null;
        }
        string = string.trim();
        if (string.isEmpty()) {
            return null;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(ISO8601_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return dateFormat.parse(string);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        Date date = new Date();
        String string = toString(date);
        System.out.println(string);
        Date date2 = toDate(string);
        System.out.println(date2);
        System.out.println(date.equals(date2));
    }
}
