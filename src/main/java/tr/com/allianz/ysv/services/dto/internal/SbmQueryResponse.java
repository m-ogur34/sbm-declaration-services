package tr.com.allianz.ysv.services.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SBM {@code ysv-beyanname/sorgu} (GET) çağrısının cevap zarfı.
 *
 * <p>POST/PUT ile aynı zarf yapısı: {@code { "result": bool, "data": {...}, "status": int } }.
 * Başarıda {@code data} sorgulanan beyannamedir ({@link SbmQueryData}); hatada
 * {@code data} yoktur ve {@link #error} dolar. Bu tip {@code DeclarationService.query}
 * tarafından çağırana aynen döndürülür.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SbmQueryResponse {

    private Boolean result;

    private Integer status;

    private SbmQueryData data;

    private SbmError error;
}
