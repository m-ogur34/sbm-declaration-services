package tr.com.allianz.ysv.services.enums;

import java.util.Locale;
import java.util.Optional;
import lombok.Getter;

/**
 * Error codes documented by SBM for the {@code ysv-beyanname} and
 * {@code ysv-beyanname/sorgu} functions.
 *
 * <p>{@code description} is the Turkish text surfaced to the operator and persisted in
 * {@code ALZ_SBM_DECL_PROCESS.ERROR_DETAILS}. {@code retryable} tells whether resending the
 * very same payload can possibly succeed: only an expired/invalid token qualifies, every
 * other code is a data or authorisation problem that must be fixed first.</p>
 */
@Getter
public enum SbmErrorCode {

    RISK_HAVUZU_00002("RISK-HAVUZU-00002",
            "Başka sigorta şirketi adına işlem yapılamaz. sigortaSirketKodu 045 olmalıdır.", false),
    RISK_HAVUZU_00003("RISK-HAVUZU-00003",
            "İlgili ay veri girişine kapalıdır. Ay kapandıktan sonra gönderim yapılamaz.", false),
    RISK_HAVUZU_00004("RISK-HAVUZU-00004",
            "Mükerrer beyanname mevcut. İl - İlçe - Yıl - Ay bazında tek kayıt gönderilebilir.", false),
    RISK_HAVUZU_00005("RISK-HAVUZU-00005",
            "Mükerrer menkul tipi gönderilemez. Menkul tipi bazında tek kayıt gönderilebilir.", false),
    RISK_HAVUZU_00006("RISK-HAVUZU-00006",
            "İl bulunamadı. Hatalı il kodu gönderilmiştir.", false),
    RISK_HAVUZU_00007("RISK-HAVUZU-00007",
            "Büyükşehirde ilçe gönderilemez. ilceKodu boş bırakılmalıdır.", false),
    RISK_HAVUZU_00008("RISK-HAVUZU-00008",
            "Büyükşehir değilse ilçe gönderilmelidir. ilceKodu zorunludur.", false),
    RISK_HAVUZU_00009("RISK-HAVUZU-00009",
            "İlçe bulunamadı. Hatalı ilçe kodu gönderilmiştir.", false),

    SEC_00001("SEC-00001",
            "Kimlik doğrulama bilgileri gönderilmelidir. Token boş gönderilmiş.", false),
    SEC_00002("SEC-00002",
            "Token geçersiz veya süresi dolmuş. Yeni token alınarak tekrar denenir.", true),
    SEC_00003("SEC-00003",
            "Bu kaynağa erişim izniniz yok.", false),
    SEC_00004("SEC-00004",
            "IP adresi doğrulanamadı. Çıkış IP adresi SBM tarafında tanımlı olmalıdır.", false),
    SEC_00005("SEC-00005",
            "Header elemanının boyutu izin verilen sınırı aşıyor.", false),
    SEC_00006("SEC-00006",
            "Zorunlu header elemanı gönderilmemiş.", false),
    SEC_00007("SEC-00007",
            "Header elemanının formatı hatalı.", false),
    SEC_00008("SEC-00008",
            "Header Requester-ID-No elemanının değeri hatalı.", false),

    CORE_00000("CORE-00000",
            "SBM sisteminde beklenmeyen bir hata oluştu. Transaction-Id ile destek talebi açılmalıdır.", false),
    CORE_00001("CORE-00001",
            "Gönderilen HTTP metodu desteklenmemektedir.", false),
    CORE_00005("CORE-00005",
            "Gönderilen alan değerinin formatı hatalı.", false),
    CORE_00006("CORE-00006",
            "Gelen veri beklenen formatta değil veya geçersiz.", false),
    CORE_00009("CORE-00009",
            "Kaynak bulunamadı. Endpoint adresi hatalı olabilir.", false),
    CORE_01000("CORE-01000",
            "Zorunlu alan boş gönderilmiş.", false),
    CORE_01001("CORE-01001",
            "Kayıt bulunamadı. Sorgu işleminde beklenen bir sonuç olabilir.", false),
    CORE_01004("CORE-01004",
            "Alan değeri izin verilen aralığın dışında.", false),
    CORE_01008("CORE-01008",
            "Alan uzunluğu izin verilen aralığın dışında.", false),

    /** Fallback for codes SBM may add later; treated as a non-retryable failure. */
    UNKNOWN("UNKNOWN",
            "SBM tarafından tanımlanmayan bir hata kodu döndü.", false);

    private final String code;
    private final String description;
    private final boolean retryable;

    SbmErrorCode(String code, String description, boolean retryable) {
        this.code = code;
        this.description = description;
        this.retryable = retryable;
    }

    /**
     * @return the matching constant, or {@link #UNKNOWN} when {@code code} is null or unmapped
     */
    public static SbmErrorCode fromCode(String code) {
        return find(code).orElse(UNKNOWN);
    }

    /**
     * @return the matching constant, or an empty optional when {@code code} is null or unmapped
     */
    public static Optional<SbmErrorCode> find(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        for (SbmErrorCode value : values()) {
            if (value.code.equals(normalized)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    /**
     * @return {@code true} when the very same request may be retried after a fresh token
     */
    public static boolean isRetryableCode(String code) {
        return find(code).map(value -> value.retryable).orElse(false);
    }

    /**
     * @return the Turkish description of {@code code}, falling back to {@link #UNKNOWN}
     */
    public static String describe(String code) {
        return fromCode(code).getDescription();
    }
}
