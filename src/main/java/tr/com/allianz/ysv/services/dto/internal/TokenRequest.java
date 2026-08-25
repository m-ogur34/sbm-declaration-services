package tr.com.allianz.ysv.services.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {

    private String clientName;

    /** A fresh {@code UUID} per call. */
    private String transactionId;

    private String functionName;

    private String userName;

    private String companyCode;
}
