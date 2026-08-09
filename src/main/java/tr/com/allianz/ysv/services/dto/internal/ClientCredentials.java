package tr.com.allianz.ysv.services.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Identity block returned by alz-token-management; it feeds the {@code Requester-ID-Type}
 * and {@code Requester-ID-No} headers SBM expects.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientCredentials {

    private Integer clientIdentityType;

    private String clientIdNumber;
}
