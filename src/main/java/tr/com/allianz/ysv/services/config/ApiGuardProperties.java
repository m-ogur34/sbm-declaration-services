package tr.com.allianz.ysv.services.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * PEN test gereksinimi (firma politikası) — uygulama uçlarının kendi savunma katmanı.
 *
 * <p>{@code ApiGuardFilter} bu ayarlarla iki şey yapar: (1) korunan yollara
 * (varsayılan {@code /api/v1/**}) {@code X-Api-Key} header'ı zorunlu kılar
 * (broken access control), (2) istemci başına istek hızını sınırlar (rate limiting,
 * aşımda HTTP 429). İç gateway'in kimlik doğrulamasının yerine değil, üstüne eklenen
 * ikinci katmandır (derinlemesine savunma).</p>
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "api-guard")
public class ApiGuardProperties {

    /** Tüm katmanı kapatır (lokal geliştirme / testler için). */
    private boolean enabled = true;

    /** API anahtarının okunacağı header adı. */
    @NotBlank
    private String apiKeyHeader = "X-Api-Key";

    /**
     * Beklenen API anahtarı. Koda gömülmez — ortam değişkeni / secret'tan gelir.
     * Boş bırakılırsa anahtar kontrolü devre dışı kalır (bir kez uyarı loglanır),
     * rate limit çalışmaya devam eder.
     */
    private String apiKey;

    /** Bu Ant desenlerine uyan yollar hiç kontrol edilmez. */
    @NotNull
    private List<String> excludedPaths = List.of(
            "/actuator/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**");

    @NotNull
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class RateLimit {

        private boolean enabled = true;

        /** Kovanın taşıyabileceği en fazla jeton (burst kapasitesi). */
        @Min(1)
        private int capacity = 60;

        /** {@link #refillPeriod} başına eklenen jeton sayısı. */
        @Min(1)
        private int refillTokens = 60;

        /** Jeton dolum periyodu. */
        @NotNull
        private Duration refillPeriod = Duration.ofMinutes(1);
    }
}
