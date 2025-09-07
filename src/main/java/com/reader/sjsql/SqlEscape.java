package com.reader.sjsql;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SqlEscape {

    private static final List<String> bad_strings = List.of(
        "'", "*", "%", ";", "+", ",", "\\"
        , "--", "/*", "*/"
        , "select", "insert", "delete", "update"
        , "union", "drop", "create", "alter");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd");

    public static Object escape(Object param) {
        if (param instanceof LocalDateTime time) {
            return time.format(DATE_TIME_FORMATTER);
        }

        if (param instanceof LocalDate date) {
            return date.format(DATE_FORMATTER);
        }

        return param;
    }

    private static String escape(String str) {
        String result = str;
        String lowerStr = result.toLowerCase();
        for (String badStr : bad_strings) {
            if (lowerStr.contains(badStr)) {
                result = result.replace(badStr, " ");
                lowerStr = result.toLowerCase();
            }
        }

        return result;
    }

}
