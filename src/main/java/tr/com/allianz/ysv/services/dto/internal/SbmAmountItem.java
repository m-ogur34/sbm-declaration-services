package tr.com.allianz.ysv.services.dto.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One element of SBM's {@code ysvTutarList}. There is exactly one element per movable type
 * inside a declaration group (SBM rejects duplicates with RISK-HAVUZU-00005).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SbmAmountItem {

    private BigDecimal alinanPrimTutari;

    private BigDecimal iptalPrimTutari;

    /** "MENKUL" or "GAYRIMENKUL". */
    private String menkulTipi;

    private BigDecimal odenecekVergi;

    private Integer vergiOrani;

    private BigDecimal vergiPrimTutari;

    /**
     * TODO(confirm): not listed in the SBM field documentation. It is sent only when the
     * database holds a value, so an unknown field can never break an ordinary request.
     */
    private BigDecimal gecmisAyIadeTutari;
}
