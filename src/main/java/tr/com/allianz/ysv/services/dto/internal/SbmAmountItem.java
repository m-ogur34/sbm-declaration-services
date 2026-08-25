package tr.com.allianz.ysv.services.dto.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SbmAmountItem {

    /** Decimal. DB: {@code RECEIVED_PREMIUM_AMOUNT}. */
    private BigDecimal alinanPrimTutari;

    /** Decimal. DB: {@code CANCELLED_PREMIUM_AMOUNT}. */
    private BigDecimal iptalPrimTutari;

    /** String, "MENKUL" or "GAYRIMENKUL". DB: {@code MOVABLE_TYPE}. */
    private String menkulTipi;

    /** Decimal. DB: {@code TAX_AMOUNT}. */
    private BigDecimal odenecekVergi;

    /** Number. DB: {@code TAX_RATIO}. */
    private Integer vergiOrani;

    /** Decimal. DB: {@code TAX_PREMIUM_AMOUNT}. */
    private BigDecimal vergiPrimTutari;

    private BigDecimal gecmisAyIadeTutari;
}
