package tr.com.allianz.ysv.services.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 4. AŞAMA — ESB (OSB katmanı) yönlendirme ayarları.
 *
 * <p>Uygulama {@code rs.sbm.org.tr} adreslerini asla doğrudan çağırmaz; her ortam için
 * tek bir ESB base-url kullanılır ({@code esb.allianz.com.tr:12000}) ve ESB isteği kendi
 * ortamına karşılık gelen SBM adresine yönlendirir. Gönder/güncelle bir proxy path'i
 * ({@code /sbmDeclarationServices}, POST/PUT), sorgu ise ayrı bir proxy path'i
 * ({@code /sbmDeclarationServicesSorgu}, GET + query string) kullanır — GET
 * parametrelerinin SBM'ye taşınabilmesi için OSB'de ayrı pipeline gerekti.</p>
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

        /**
         * Gönder/güncelle için ESB (OSB) Proxy Service endpoint URI'si
         * ({@code /sbmDeclarationServices}). Proxy, SBM'nin gerçek adresine
         * ({@code .../api/rest/vergi-beyan-rs/v10/ysv-beyanname}) yönlendirir. Tüm Allianz
         * OSB ortamlarında aynıdır; ortam farkı sadece {@code base-url}'dedir.
         */
        @NotBlank
        private String beyannamePath = "/sbmDeclarationServices";

        /**
         * Sorgu (GET) için <b>ayrı</b> Proxy Service path'i
         * ({@code /sbmDeclarationServicesSorgu}). GET'in query parametrelerini
         * ({@code sigortaSirketKodu}, {@code ysvDosyaNo}) SBM'ye taşıyabilmek için
         * gönder/güncelle proxy'sinden ayrı bir pipeline'a bağlıdır.
         */
        @NotBlank
        private String sorguPath = "/sbmDeclarationServicesSorgu";
    }
}
