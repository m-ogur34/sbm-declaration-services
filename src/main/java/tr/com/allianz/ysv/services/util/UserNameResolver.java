package tr.com.allianz.ysv.services.util;

import java.nio.charset.StandardCharsets;

/**
 * Validates the {@code X-User-Name} header before it is written to the audit columns.
 *
 * <p>The header is filled by the internal gateway but still arrives from the client, and it
 * lands unchanged in {@code CREATED_BY_USER} / {@code SENT_BY_USER} / {@code UPDATED_BY_USER},
 * all VARCHAR2(100) declared in bytes. An oversized value would abort the whole operation
 * with ORA-12899 - a 580 row import included - so it is refused at the boundary with HTTP 400
 * instead.</p>
 */
public final class UserNameResolver {

    /** Byte budget of the audit columns; Turkish characters cost two bytes each. */
    public static final int MAX_BYTES = 100;

    private UserNameResolver() {
    }

    /**
     * @param userName raw header value, may be {@code null}
     * @return the trimmed user name, or {@code null} when the header was absent
     * @throws IllegalArgumentException when the value does not fit the audit columns
     */
    public static String resolve(String userName) {
        if (userName == null) {
            return null;
        }
        String trimmed = userName.trim();
        if (trimmed.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalArgumentException(
                    "X-User-Name en fazla " + MAX_BYTES + " karakter olabilir.");
        }
        return trimmed;
    }
}
