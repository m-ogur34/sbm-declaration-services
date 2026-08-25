package tr.com.allianz.ysv.services.dto.response;

import java.util.List;

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
