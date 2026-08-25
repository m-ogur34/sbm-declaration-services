package tr.com.allianz.ysv.services.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Single error contract of the whole API.
 *
 * @param timestamp when the failure was rendered
 * @param path      request path that failed
 * @param code      stable error code (SBM code or an internal one)
 * @param message   Turkish message for the operator
 * @param details   field level details, empty when there are none
 */
public record ErrorResponse(LocalDateTime timestamp,
                            String path,
                            String code,
                            String message,
                            List<String> details) {

    public static ErrorResponse of(String path, String code, String message, List<String> details) {
        return new ErrorResponse(LocalDateTime.now(), path, code, message,
                details == null ? List.of() : List.copyOf(details));
    }
}
