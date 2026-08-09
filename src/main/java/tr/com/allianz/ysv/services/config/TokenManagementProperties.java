package tr.com.allianz.ysv.services.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration of the alz-token-management service. A brand new token is requested before
 * every SBM call; this application deliberately keeps no token cache.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "token-management")
public class TokenManagementProperties {

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String path = "/alz-token-management/api/v1/tokens/sbm-token-generate";

    @NotBlank
    private String clientName;

    @NotBlank
    private String userName;

    @NotBlank
    private String companyCode;

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(30);

    /** @return absolute URL of the token generation endpoint */
    public String tokenUrl() {
        return baseUrl + path;
    }
}
