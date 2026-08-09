package tr.com.allianz.ysv.services.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response of alz-token-management. Never logged as-is: the access token and the identity
 * number are masked before they reach any appender.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenResponse {

    private String accessToken;

    private ClientCredentials clientCredentials;
}
