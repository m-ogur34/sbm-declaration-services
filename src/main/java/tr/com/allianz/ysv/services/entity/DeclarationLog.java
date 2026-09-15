package tr.com.allianz.ysv.services.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tr.com.allianz.ysv.services.enums.LogLevel;
import tr.com.allianz.ysv.services.enums.OperationType;

/**
 * One row of {@code CUSTOMER.ALZ_SBM_DECL_LOG}: the legally required evidence of every REST
 * call made against SBM, written for successful and failed calls alike.
 */
@Entity
@Table(name = "ALZ_SBM_DECL_LOG")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclarationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "declLogSeqGenerator")
    @SequenceGenerator(name = "declLogSeqGenerator",
            sequenceName = "ALZ_SBM_DECL_LOG_SEQ",
            allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PROCESS_ID")
    private Long processId;

    @Enumerated(EnumType.STRING)
    @Column(name = "OPERATION_TYPE", length = 30)
    private OperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "LOG_LEVEL", length = 50)
    private LogLevel logLevel;

    @Lob
    @Column(name = "LOG_MESSAGE")
    private String logMessage;

    @Lob
    @Column(name = "REQUEST_PAYLOAD")
    private String requestPayload;

    @Lob
    @Column(name = "RESPONSE_PAYLOAD")
    private String responsePayload;

    @Column(name = "DATE_CREATED")
    private LocalDateTime dateCreated;

    /**
     * Operator behind the call, taken from the {@code X-User-Name} header. It is an audit
     * label, not an identity: the header is not authenticated, so nothing may authorize on it.
     * Until the UI is wired up every call arrives as {@code SYSTEM}.
     */
    @Column(name = "TRIGGERED_BY_USER", length = 100)
    private String triggeredByUser;
}
