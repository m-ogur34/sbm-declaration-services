package tr.com.allianz.ysv.services.exception;

import java.io.Serial;

/**
 * Raised when alz-token-management cannot deliver a usable token. Without a token no SBM
 * call can even be attempted, so this is reported separately from SBM's own errors.
 */
public class TokenException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
