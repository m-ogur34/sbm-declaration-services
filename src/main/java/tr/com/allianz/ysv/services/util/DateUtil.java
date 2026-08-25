package tr.com.allianz.ysv.services.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Date helpers for the SBM contract, which uses {@code yyyy-MM-dd} everywhere.
 */
public final class DateUtil {

    public static final DateTimeFormatter SBM_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateUtil() {
    }

    /**
     * @param date date to render, may be {@code null}
     * @return {@code yyyy-MM-dd} representation, or {@code null}
     */
    public static String format(LocalDate date) {
        return date == null ? null : date.format(SBM_DATE_FORMATTER);
    }

    /**
     * @param value {@code yyyy-MM-dd} text, may be {@code null} or blank
     * @return parsed date, or {@code null} when the text is empty or not parsable
     */
    public static LocalDate parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), SBM_DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
