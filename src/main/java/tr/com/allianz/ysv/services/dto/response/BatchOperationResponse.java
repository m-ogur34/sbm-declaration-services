package tr.com.allianz.ysv.services.dto.response;

import java.util.List;

/**
 * Result of a send / update / cancel batch.
 *
 * @param totalGroups  number of SBM requests the batch resolved to
 * @param successCount groups accepted by SBM
 * @param failCount    groups rejected by SBM or by local validation
 * @param failures     details of the failed groups
 */
public record BatchOperationResponse(int totalGroups,
                                     int successCount,
                                     int failCount,
                                     List<FailureDetail> failures) {

    public static BatchOperationResponse of(int totalGroups, List<FailureDetail> failures) {
        List<FailureDetail> safeFailures = failures == null ? List.of() : List.copyOf(failures);
        return new BatchOperationResponse(totalGroups,
                totalGroups - safeFailures.size(),
                safeFailures.size(),
                safeFailures);
    }
}
