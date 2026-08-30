package tr.com.allianz.ysv.services.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SBM {@code ysv-beyanname} POST/PUT çağrılarının cevabı — hem başarı hem 4xx/5xx gövdesi.
 *
 * <p>SBM her işlemde aynı zarfı döner: {@code { "result": bool, "data": <...>, "status": int } }.
 * {@code data} polimorfiktir: POST'ta {@code { "ysvDosyaNo": "..." }} nesnesi, PUT'ta
 * {@code true} boolean, hata durumunda hiç yok. Bu yüzden {@code data} ham {@link JsonNode}
 * olarak tutulur; {@link #extractYsvDosyaNo()} POST cevabından dosya numarasını çıkarır.
 * Başarı ölçütü {@code SbmClientService} içinde: HTTP 2xx ve {@code result == true}.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SbmDeclarationResponse {

    private Boolean result;

    private Integer status;

    /** POST: {@code { "ysvDosyaNo": "..." }} — PUT: {@code true} — hata: yok. */
    private JsonNode data;

    private SbmError error;

    /**
     * @return POST cevabındaki {@code data.ysvDosyaNo}, yoksa {@code null}
     */
    public String extractYsvDosyaNo() {
        if (data != null && data.hasNonNull("ysvDosyaNo")) {
            return data.get("ysvDosyaNo").asText();
        }
        return null;
    }
}
