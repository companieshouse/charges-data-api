package uk.gov.companieshouse.charges.data.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeFormatter {

    static final String strPattern = "\\d{4}-\\d{2}-\\d{2}";
    static final Pattern pattern = Pattern.compile(strPattern);

    static java.time.format.DateTimeFormatter writeDateTimeFormatter =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static java.time.format.DateTimeFormatter readDateTimeFormatter =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

    static java.time.format.DateTimeFormatter publishedAtDateTimeFormatter =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Parse date string to LocalDate.
     * @param dateStr date as string.
     * @return parsed date.
     */
    public static LocalDate parse(String dateStr) {
        Matcher matcher = pattern.matcher(dateStr);
        matcher.find();
        return LocalDate.parse(matcher.group(), readDateTimeFormatter);
    }

    /**
     * Format date.
     * @param localDate date to format.
     * @return formatted date as string.
     */
    public static String format(LocalDate localDate) {
        return localDate.atStartOfDay().format(writeDateTimeFormatter);
    }

    /**
     * Format publishedAt date
     * @param now current time as Instant
     * @return UTC time as string rounded to seconds
     */
    public static String formatPublishedAt(Instant now) {
        return publishedAtDateTimeFormatter.format(now.atZone(ZoneOffset.UTC));
    }
}
