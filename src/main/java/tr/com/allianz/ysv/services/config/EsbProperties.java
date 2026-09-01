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
 * 4. AŞAMA — ESB (OSB katmanı) yönlendirme ayarları.
 *
 * <p>Uygulama {@code rs.sbm.org.tr} adreslerini asla doğrudan çağırmaz; her ortam için
 * tek bir ESB base-url kullanılır ({@code esb.allianz.com.tr:12000}) ve ESB isteği kendi
 * ortamına karşılık gelen SBM adresine yönlendirir. Gönder/güncelle ve sorgu <b>aynı</b>
 * path'tedir; sorgu sadece HTTP GET olması ve parametreleri query string ile taşımasıyla
 * ayrılır.</p>
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

        /**
         * SBM dökümanında sorgu, gönderle aynı path üzerinde GET metodudur (ayrı bir
         * {@code /sorgu} eki yoktur). ESB proxy path'i farklıysa ortam config'inden ezilir.
         */
        @NotBlank
        private String sorguPath = "/api/rest/vergi-beyan-rs/v10/ysv-beyanname";

        /**
         * Sorgu isteğinin ESB proxy'sine hangi HTTP metoduyla gideceği. SBM tarafında sorgu
         * fonksiyonu GET'tir, ancak ESB proxy'si tek path üzerinden çalıştığı için GET yerine
         * POST ile yönlendirme bekleyebilir. İkisi de aynı JSON gövdeyi
         * ({@code {sigortaSirketKodu, ysvDosyaNo}}) taşır. Ortam config'inden değiştirilir.
         */
        @Pattern(regexp = "GET|POST")
        private String sorguMethod = "GET";
    }
}
