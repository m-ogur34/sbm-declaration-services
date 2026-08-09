package tr.com.allianz.ysv.services.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * ESB (OSB layer) routing configuration. The application never talks to rs.sbm.org.tr
 * directly; a single ESB base URL is used for every environment and the ESB itself routes
 * to the matching SBM endpoint.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "esb")
public class EsbProperties {

    /** Single ESB entry point, e.g. {@code http://esb.allianz.com.tr:12000}. */
    @NotBlank
    private String baseUrl;

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(10);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(60);

    @NotNull
    private Ysv ysv = new Ysv();

    /** @return absolute URL of the send/update endpoint */
    public String beyannameUrl() {
        return baseUrl + ysv.getBeyannamePath();
    }

    /** @return absolute URL of the query endpoint */
    public String sorguUrl() {
        return baseUrl + ysv.getSorguPath();
    }

    @Getter
    @Setter
    public static class Ysv {

        @NotBlank
        private String beyannamePath = "/api/rest/vergi-beyan-rs/v10/ysv-beyanname";

        @NotBlank
        private String sorguPath = "/api/rest/vergi-beyan-rs/v10/ysv-beyanname/sorgu";

        /**
         * TODO(confirm): the SBM document describes the query function as GET but shows a
         * request with a body. Keep this switchable until SBM confirms which verb the ESB
         * forwards; both variants are implemented in SbmClientService.
         */
        @Pattern(regexp = "GET|POST")
        private String sorguMethod = "GET";
    }
}
