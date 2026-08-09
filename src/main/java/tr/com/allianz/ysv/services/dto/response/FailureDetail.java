package tr.com.allianz.ysv.services.dto.response;

/**
 * A single failed group inside a batch operation result.
 *
 * @param ysvDosyaNo SBM file number of the failed group
 * @param errorCode  SBM error code, or an internal validation code
 * @param message    Turkish explanation shown to the operator
 */
public record FailureDetail(String ysvDosyaNo, String errorCode, String message) {
}
