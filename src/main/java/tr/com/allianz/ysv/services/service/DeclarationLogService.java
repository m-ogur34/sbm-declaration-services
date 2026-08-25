package tr.com.allianz.ysv.services.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tr.com.allianz.ysv.services.entity.DeclarationLog;
import tr.com.allianz.ysv.services.enums.LogLevel;
import tr.com.allianz.ysv.services.enums.OperationType;
import tr.com.allianz.ysv.services.repository.DeclarationLogRepository;

/**
 * Writes the legally required evidence of every SBM call into {@code ALZ_SBM_DECL_LOG}.
 *
 * <p>The write runs in its own transaction so that a rollback of the declaration
 * transaction can never take the evidence with it. Nothing here may abort the caller: if
 * the insert itself fails, the payload is written to the application log instead, because
 * the SBM call has already happened and re-driving it would risk a duplicate declaration.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeclarationLogService {

    private final DeclarationLogRepository declarationLogRepository;

    /**
     * @param processIds      rows of the declaration group, one log row is written per id
     * @param operationType   POST / PUT / GET
     * @param logLevel        INFO for accepted calls, ERROR otherwise
     * @param message         short Turkish summary
     * @param requestPayload  serialized request body; never contains the Authorization header
     * @param responsePayload raw response body
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCall(Collection<Long> processIds,
                        OperationType operationType,
                        LogLevel logLevel,
                        String message,
                        String requestPayload,
                        String responsePayload) {
        LocalDateTime now = LocalDateTime.now();
        List<DeclarationLog> rows = new ArrayList<>();
        if (processIds == null || processIds.isEmpty()) {
            rows.add(buildRow(null, operationType, logLevel, message, requestPayload, responsePayload, now));
        } else {
            for (Long processId : processIds) {
                rows.add(buildRow(processId, operationType, logLevel, message,
                        requestPayload, responsePayload, now));
            }
        }

        try {
            declarationLogRepository.saveAll(rows);
        } catch (RuntimeException ex) {
            log.error("SBM audit log could not be persisted (operation={}, processIds={}). "
                            + "message={}, request={}, response={}",
                    operationType, processIds, message, requestPayload, responsePayload, ex);
        }
    }

    private DeclarationLog buildRow(Long processId,
                                    OperationType operationType,
                                    LogLevel logLevel,
                                    String message,
                                    String requestPayload,
                                    String responsePayload,
                                    LocalDateTime createdAt) {
        return DeclarationLog.builder()
                .processId(processId)
                .operationType(operationType)
                .logLevel(logLevel)
                .logMessage(message)
                .requestPayload(requestPayload)
                .responsePayload(responsePayload)
                .dateCreated(createdAt)
                .build();
    }
}
