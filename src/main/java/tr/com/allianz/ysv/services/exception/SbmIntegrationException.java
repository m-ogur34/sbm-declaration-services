package tr.com.allianz.ysv.services.exception;

import java.io.Serial;
import lombok.Getter;

/**
 * Raised when a declaration cannot be transferred to SBM. The error code is either an SBM
 * code echoed back from the remote side, or - for pre-flight validation - the SBM code the
 * request would have been rejected with (for example RISK-HAVUZU-00005 for a duplicate
 * movable type), so that operators always see one consistent code vocabulary.
 */
@Getter
public class SbmIntegrationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public SbmIntegrationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
