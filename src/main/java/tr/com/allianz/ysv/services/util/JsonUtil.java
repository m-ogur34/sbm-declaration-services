package tr.com.allianz.ysv.services.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JSON helpers for the audit log. Serialization problems are never allowed to abort a
 * declaration: a placeholder is stored instead, because the transfer itself already
 * happened and the log row is legal evidence.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonUtil {

    /** {@code ALZ_SBM_DECL_PROCESS.ERROR_DETAILS} is VARCHAR2(2000). */
    public static final int ERROR_DETAILS_MAX_LENGTH = 2000;

    private static final String UNSERIALIZABLE = "{\"error\":\"payload serialize edilemedi\"}";

    private final ObjectMapper objectMapper;

    /**
     * @param value object to serialize, may be {@code null}
     * @return JSON text, {@code null} for a {@code null} input, or a placeholder on failure
     */
    public String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Payload could not be serialized to JSON: {}", ex.getMessage());
            return UNSERIALIZABLE;
        }
    }

    /**
     * @param json JSON text, may be {@code null} or blank
     * @param type target type
     * @return the parsed object, or {@code null} when the text is empty or malformed
     */
    public <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            log.warn("Response body could not be parsed as {}: {}", type.getSimpleName(), ex.getMessage());
            return null;
        }
    }

    /**
     * @param value text to shorten, may be {@code null}
     * @param maxLength maximum number of characters to keep
     * @return {@code value} unchanged when it fits, otherwise the truncated text
     */
    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
