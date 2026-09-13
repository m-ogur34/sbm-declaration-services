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
 *
 * <p>Every value here is environment specific and carries no default on purpose: only the
 * SC-TEST parameters are known today. A profile that has not been filled in fails Bean
 * Validation and the application refuses to start, which is far easier to diagnose than a
 * token call that only fails once someone triggers a declaration.</p>
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "token-management")
public class TokenManagementProperties {

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String path;

    @NotBlank
    private String clientName;

    /**
     * alz-token-management {@code functionName} alanı. Bugün tüm ortamlardan {@code "test"}
     * ile token alınabiliyor; token ekibi operasyona özel isim isterse ortam config'inden
     * ezilir. Kod bunu operasyona göre değiştirmez.
     */
    @NotBlank
    private String functionName = "test";

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
