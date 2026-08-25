package tr.com.allianz.ysv.services.util;

/**
 * Masking of credentials and identity numbers. Tokens and identity numbers must never reach
 * an appender unmasked.
 */
public final class MaskUtil {

    private static final int VISIBLE_PREFIX_LENGTH = 10;
    private static final String SUFFIX = "***";

    private MaskUtil() {
    }

    /**
     * @param value secret value, may be {@code null}
     * @return the first 10 characters followed by {@code ***}, or {@code null}
     */
    public static String mask(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= VISIBLE_PREFIX_LENGTH) {
            return SUFFIX;
        }
        return value.substring(0, VISIBLE_PREFIX_LENGTH) + SUFFIX;
    }
}
