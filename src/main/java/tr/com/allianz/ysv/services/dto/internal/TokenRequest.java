package tr.com.allianz.ysv.services.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body of the alz-token-management {@code sbm-token-generate} call.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {

    private String clientName;

    /** A fresh {@code UUID} per call. */
    private String transactionId;

    /**
     * TODO(confirm): the sample uses "test". The operation names sent here
     * (ysv-beyanname-gonder / -guncelle / -sorgu) still need confirmation from the
     * alz-token-management team.
     */
    private String functionName;

    private String userName;

    private String companyCode;
}
