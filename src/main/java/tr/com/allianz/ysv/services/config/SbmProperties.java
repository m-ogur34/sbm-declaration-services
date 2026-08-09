package tr.com.allianz.ysv.services.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * SBM business configuration.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "sbm")
public class SbmProperties {

    /**
     * Allianz Sigorta company code as registered at SBM ("045"). The 2320 value found in the
     * OPUS extract is an internal code and must never be sent to SBM.
     */
    @NotBlank
    private String companyCode;

    @NotNull
    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Retry {

        /**
         * Total number of attempts (1 = no retry). Only SEC-00002 (expired token) and 5xx
         * responses are retried; every other failure is a data problem.
         */
        @Min(1)
        private int maxAttempts = 2;
    }
}
