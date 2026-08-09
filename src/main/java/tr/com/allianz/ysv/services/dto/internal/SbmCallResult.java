package tr.com.allianz.ysv.services.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Outcome of a single SBM call, carrying everything the audit log and the process status
 * update need. Transport failures are represented here as well, so callers never have to
 * catch exceptions from {@code SbmClientService}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SbmCallResult {

    /** HTTP 2xx and {@code result:true}. */
    private boolean success;

    /** HTTP status, or 0 when the call never produced a response. */
    private int httpStatus;

    /** SBM's {@code Transaction-Id} response header; required for SBM support tickets. */
    private String transactionId;

    /** Serialized request body, stored in {@code ALZ_SBM_DECL_LOG.REQUEST_PAYLOAD}. */
    private String requestPayload;

    /** Raw response body, stored in {@code ALZ_SBM_DECL_LOG.RESPONSE_PAYLOAD}. */
    private String responsePayload;

    /** First SBM reason code, e.g. {@code CORE-01004}. */
    private String errorCode;

    /** Turkish, operator facing explanation of the failure. */
    private String errorMessage;

    /** Echoed back by SBM on a successful POST. */
    private String ysvDosyaNo;
}
