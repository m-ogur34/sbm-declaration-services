package tr.com.allianz.ysv.services.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "sbm")
public class SbmProperties {

    @NotBlank
    private String companyCode;

    @NotNull
    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Retry {

        @Min(1)
        private int maxAttempts = 2;
    }
}
